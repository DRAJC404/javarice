from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.database import get_db
from app.models.release import Release
from app.schemas.release import ReleaseResponse

router = APIRouter(prefix="/api/releases", tags=["releases"])


@router.get("/latest", response_model=ReleaseResponse)
async def get_latest_release(db: AsyncSession = Depends(get_db)):
    """Retrieve the most recent published production release with its installer assets."""
    stmt = (
        select(Release)
        .options(selectinload(Release.assets))
        .where(Release.is_published == True)
        .order_by(desc(Release.published_at))
        .limit(1)
    )
    result = await db.execute(stmt)
    release = result.scalars().first()

    if not release:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No published releases found.",
        )

    return release


@router.get("", response_model=List[ReleaseResponse])
async def list_releases(
    limit: int = 20,
    offset: int = 0,
    db: AsyncSession = Depends(get_db),
):
    """List all published releases ordered from newest to oldest for the changelog."""
    stmt = (
        select(Release)
        .options(selectinload(Release.assets))
        .where(Release.is_published == True)
        .order_by(desc(Release.published_at))
        .offset(offset)
        .limit(min(limit, 100))
    )
    result = await db.execute(stmt)
    releases = result.scalars().all()
    return releases
