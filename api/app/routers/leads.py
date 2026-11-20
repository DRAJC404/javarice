from fastapi import APIRouter, Depends, Request, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.database import get_db
from app.models.lead import Lead
from app.schemas.lead import LeadResponse, SendLinkRequest
from app.utils.rate_limiter import get_client_ip, leads_rate_limiter
from app.utils.security import hash_ip

router = APIRouter(prefix="/api/leads", tags=["leads"])


@router.post("/send-link", response_model=LeadResponse, status_code=status.HTTP_201_CREATED)
async def send_download_link(
    payload: SendLinkRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    """
    Store mobile visitor's email lead and trigger download link delivery.
    Enforces sliding-window rate limiting and stores consent flags.
    """
    client_ip = get_client_ip(request)
    leads_rate_limiter.check(client_ip)

    ip_hashed = hash_ip(client_ip)

    lead = Lead(
        email=str(payload.email).strip().lower(),
        platform=payload.platform,
        arch=payload.arch,
        consent=payload.consent,
        ip_hash=ip_hashed,
    )
    db.add(lead)
    await db.commit()

    return LeadResponse(
        message="Download link successfully sent! Check your email to install bAI.",
        email=str(payload.email),
        success=True,
    )
