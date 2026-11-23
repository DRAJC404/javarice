from typing import Optional
from fastapi import APIRouter, Depends, Query
from packaging import version as pkg_version
from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.database import get_db
from app.models.release import Release
from app.routers.downloads import normalize_arch, normalize_platform
from app.schemas.update import UpdateCheckResponse

router = APIRouter(prefix="/api/updates", tags=["updates"])


def is_newer_version(latest_str: str, current_str: str) -> bool:
    try:
        clean_latest = latest_str.lstrip("v")
        clean_current = current_str.lstrip("v")
        return pkg_version.parse(clean_latest) > pkg_version.parse(clean_current)
    except Exception:
        return clean_latest != clean_current


@router.get("/check", response_model=UpdateCheckResponse)
async def check_for_updates(
    version: str = Query(..., description="Currently installed client version, e.g. 1.0.0"),
    platform: Optional[str] = Query(None, description="Operating system: windows, macos, linux"),
    arch: Optional[str] = Query(None, description="Architecture: x64, arm64"),
    db: AsyncSession = Depends(get_db),
):
    """
    Check for software updates against the latest published production release.
    Used by the JavaFX client auto-updater.
    """
    stmt = (
        select(Release)
        .options(selectinload(Release.assets))
        .where(Release.is_published == True, Release.is_prerelease == False)
        .order_by(desc(Release.published_at))
        .limit(1)
    )
    result = await db.execute(stmt)
    latest = result.scalars().first()

    if not latest:
        return UpdateCheckResponse(
            update_available=False,
            current_version=version,
            latest_version=version,
        )

    has_update = is_newer_version(latest.version, version)

    download_url = None
    sha256_hash = None

    if platform and latest.assets:
        norm_p = normalize_platform(platform)
        norm_a = normalize_arch(arch) if arch else None

        matching = [
            a
            for a in latest.assets
            if a.platform == norm_p and (not norm_a or a.arch == norm_a or a.arch == "all")
        ]
        if matching:
            download_url = matching[0].download_url
            sha256_hash = matching[0].sha256

    return UpdateCheckResponse(
        update_available=has_update,
        current_version=version,
        latest_version=latest.version,
        download_url=download_url,
        release_notes=latest.notes,
        sha256=sha256_hash,
        published_at=latest.published_at,
    )
