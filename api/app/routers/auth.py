import re
from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.database import get_db, set_tenant_context
from app.models.audit_log import AuditLog
from app.models.join_code import JoinCode
from app.models.organization import Organization
from app.models.rate_limit import JoinAttemptRateLimit
from app.models.user import User
from app.schemas.auth import (
    AuthResponse,
    JoinRequest,
    LoginRequest,
    OrgResponse,
    RefreshTokenRequest,
    RegisterBusinessRequest,
    TokenRefreshResponse,
    UserResponse,
)
from app.utils.rate_limiter import auth_rate_limiter, get_client_ip
from app.utils.security import (
    create_access_token,
    decode_access_token,
    generate_remember_token,
    hash_password,
    normalize_join_code,
    sha256_hex,
    verify_password,
)

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/register-business", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
async def register_business(
    payload: RegisterBusinessRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    Register a new business organization and its OWNER user in a single atomic transaction.
    Enforces tenant isolation and records an OWNER_REGISTERED audit log entry.
    """
    clean_email = payload.email.strip().lower()
    existing = await db.execute(select(User).where(func.lower(User.email) == clean_email))
    if existing.scalars().first():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="An account with this email address already exists. Try signing in instead.",
        )
    clean_name = payload.business_name.strip()
    slug = re.sub(r"[^a-z0-9]+", "-", clean_name.lower()).strip("-")
    if not slug:
        slug = f"org-{int(datetime.now(timezone.utc).timestamp())}"
    slug_check = await db.execute(select(Organization).where(Organization.slug == slug))
    if slug_check.scalars().first():
        slug = f"{slug}-{int(datetime.now(timezone.utc).timestamp())}"
    org = Organization(name=clean_name, slug=slug, is_active=True)
    db.add(org)
    await db.flush()
    await set_tenant_context(db, org.id)
    pwd_hash = hash_password(payload.password)
    user = User(
        organization_id=org.id,
        role_id=1,
        full_name=payload.full_name.strip(),
        email=clean_email,
        password_hash=pwd_hash,
        approval_status="APPROVED",
        is_active=True,
    )
    db.add(user)
    await db.flush()
    audit = AuditLog(
        organization_id=org.id,
        user_id=user.id,
        action="OWNER_REGISTERED",
        details=f"Business owner account registered for {clean_name}",
    )
    db.add(audit)
    await db.commit()
    await db.refresh(user)
    await db.refresh(org)
    access_token = create_access_token(
        {"sub": str(user.id), "org_id": org.id, "role_id": user.role_id, "email": user.email}
    )
    raw_token, token_hash, expires_at = generate_remember_token()
    user.remember_token_hash = token_hash
    user.remember_expires_at = expires_at
    await db.commit()

    return AuthResponse(
        access_token=access_token,
        refresh_token=raw_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        organization=OrgResponse.model_validate(org),
    )


@router.post("/login", response_model=AuthResponse)
async def login(
    payload: LoginRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    """
    Authenticate user credentials, verify approval status and active state,
    and return session access & refresh tokens.
    """
    client_ip = get_client_ip(request)
    auth_rate_limiter.check(client_ip)

    clean_email = payload.email.strip().lower()
    stmt = (
        select(User)
        .options(selectinload(User.organization))
        .where(func.lower(User.email) == clean_email)
    )
    result = await db.execute(stmt)
    user = result.scalars().first()

    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password.",
        )

    if user.approval_status != "APPROVED":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Account approval status is {user.approval_status}. Please contact your administrator.",
        )

    if not user.is_active or not user.organization.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Account or organization has been deactivated.",
        )
    await set_tenant_context(db, user.organization_id)

    raw_refresh_token = None
    if payload.remember_me:
        raw_refresh_token, token_hash, expires_at = generate_remember_token()
        user.remember_token_hash = token_hash
        user.remember_expires_at = expires_at
        await db.commit()

    access_token = create_access_token(
        {
            "sub": str(user.id),
            "org_id": user.organization_id,
            "role_id": user.role_id,
            "email": user.email,
        }
    )

    return AuthResponse(
        access_token=access_token,
        refresh_token=raw_refresh_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        organization=OrgResponse.model_validate(user.organization),
    )


@router.post("/refresh", response_model=TokenRefreshResponse)
async def refresh_token(
    payload: RefreshTokenRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    Exchange a valid remember token for a new access token.
    Matches fn_find_user_by_remember_token logic using SHA-256 hash lookup.
    """
    token_str = payload.refresh_token.strip()
    jwt_payload = decode_access_token(token_str)
    if jwt_payload and "sub" in jwt_payload:
        user_id = int(jwt_payload["sub"])
        res = await db.execute(select(User).where(User.id == user_id))
        user = res.scalars().first()
        if user and user.is_active and user.approval_status == "APPROVED":
            new_token = create_access_token(
                {"sub": str(user.id), "org_id": user.organization_id, "role_id": user.role_id, "email": user.email}
            )
            return TokenRefreshResponse(access_token=new_token)
    token_hash = sha256_hex(token_str)
    now = datetime.now(timezone.utc)
    stmt = select(User).where(
        User.remember_token_hash == token_hash,
        User.remember_expires_at > now,
        User.is_active == True,
    )
    result = await db.execute(stmt)
    user = result.scalars().first()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired refresh token",
        )

    new_token = create_access_token(
        {"sub": str(user.id), "org_id": user.organization_id, "role_id": user.role_id, "email": user.email}
    )
    return TokenRefreshResponse(access_token=new_token)


@router.post("/join", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
async def redeem_join_code(
    payload: JoinRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    """
    Redeem an 8-character invitation join code to join an organization.
    Checks rate limits in join_attempt_rate_limits, validates expiry, role,
    updates code as used, writes audit log, and creates staff user.
    """
    device_id = payload.device_id or get_client_ip(request)
    one_min_ago = datetime.now(timezone.utc) - timedelta(minutes=1)
    attempt_count_query = select(func.count(JoinAttemptRateLimit.id)).where(
        JoinAttemptRateLimit.device_id == device_id,
        JoinAttemptRateLimit.attempt_time >= one_min_ago,
    )
    attempt_count_res = await db.execute(attempt_count_query)
    attempts = attempt_count_res.scalar() or 0
    rate_limit_record = JoinAttemptRateLimit(device_id=device_id)
    db.add(rate_limit_record)
    await db.flush()

    if attempts >= 10:
        await db.commit()
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Too many join attempts. Please wait 1 minute before trying again.",
        )
    cleaned_code = normalize_join_code(payload.code)
    if len(cleaned_code) != 8 or not re.match(r"^[A-Z0-9]{8}$", cleaned_code):
        await db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="The code should be 8 characters, like G87M-6MZK.",
        )
    code_hash = sha256_hex(cleaned_code)
    stmt = (
        select(JoinCode)
        .options(selectinload(JoinCode.organization))
        .where(JoinCode.code_hash == code_hash)
        .with_for_update()
    )
    result = await db.execute(stmt)
    join_code = result.scalars().first()

    now = datetime.now(timezone.utc)
    if not join_code:
        await db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="We couldn't find that code. Please check it and try again.",
        )

    if join_code.used_at is not None:
        await db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This code has already been used.",
        )

    if join_code.cancelled_at is not None or (
        join_code.expires_at.replace(tzinfo=timezone.utc) if join_code.expires_at.tzinfo is None else join_code.expires_at
    ) < now:
        await db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This code has expired. Ask your admin to generate a new one.",
        )

    if not join_code.organization or not join_code.organization.is_active:
        await db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="The organization associated with this code is inactive.",
        )
    clean_email = payload.email.strip().lower()
    email_check = await db.execute(select(User).where(func.lower(User.email) == clean_email))
    if email_check.scalars().first():
        await db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="An account with this email already exists. Try signing in instead.",
        )
    await set_tenant_context(db, join_code.organization_id)
    role_id = 2 if join_code.role == "MANAGER" else 3
    pwd_hash = hash_password(payload.password)
    user = User(
        organization_id=join_code.organization_id,
        role_id=role_id,
        full_name=payload.full_name.strip(),
        email=clean_email,
        password_hash=pwd_hash,
        approval_status="APPROVED",
        is_active=True,
    )
    db.add(user)
    await db.flush()
    join_code.used_at = now
    join_code.used_by = user.id
    audit = AuditLog(
        organization_id=join_code.organization_id,
        user_id=user.id,
        action="CODE_REDEEMED",
        details=f"Staff member {payload.full_name.strip()} joined as {join_code.role}",
    )
    db.add(audit)
    await db.commit()
    await db.refresh(user)
    await db.refresh(join_code.organization)
    access_token = create_access_token(
        {
            "sub": str(user.id),
            "org_id": user.organization_id,
            "role_id": user.role_id,
            "email": user.email,
        }
    )

    return AuthResponse(
        access_token=access_token,
        token_type="bearer",
        user=UserResponse.model_validate(user),
        organization=OrgResponse.model_validate(join_code.organization),
    )
