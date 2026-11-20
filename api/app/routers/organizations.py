from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import get_db, set_tenant_context
from app.models.audit_log import AuditLog
from app.models.join_code import JoinCode
from app.models.user import User
from app.schemas.organization import CreateJoinCodeRequest, JoinCodeResponse
from app.utils.dependencies import get_current_owner
from app.utils.security import generate_join_code, normalize_join_code, sha256_hex

router = APIRouter(prefix="/api/organizations", tags=["organizations"])


@router.post("/{org_id}/join-codes", response_model=JoinCodeResponse, status_code=status.HTTP_201_CREATED)
async def create_join_code(
    org_id: int,
    payload: CreateJoinCodeRequest,
    current_owner: User = Depends(get_current_owner),
    db: AsyncSession = Depends(get_db),
):
    """
    Generate a 10-minute invitation join code for staff onboarding.
    Requires OWNER / ADMIN role and enforces tenant boundary isolation.
    """
    if current_owner.organization_id != org_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cannot generate join codes for an organization other than your own.",
        )

    await set_tenant_context(db, org_id)

    raw_code = generate_join_code()
    cleaned = normalize_join_code(raw_code)
    code_hash = sha256_hex(cleaned)

    now = datetime.now(timezone.utc)
    expires_at = now + timedelta(minutes=10)

    join_code = JoinCode(
        organization_id=org_id,
        code_hash=code_hash,
        role=payload.role,
        created_by=current_owner.id,
        expires_at=expires_at,
    )
    db.add(join_code)
    await db.flush()

    audit = AuditLog(
        organization_id=org_id,
        user_id=current_owner.id,
        action="JOIN_CODE_CREATED",
        details=f"Generated {payload.role} invite code valid until {expires_at.isoformat()}",
    )
    db.add(audit)
    await db.commit()
    await db.refresh(join_code)

    return JoinCodeResponse(
        id=join_code.id,
        organization_id=join_code.organization_id,
        code=raw_code,
        role=join_code.role,
        expires_at=join_code.expires_at,
        created_at=join_code.created_at,
    )
