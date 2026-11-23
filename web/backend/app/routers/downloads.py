from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from fastapi.responses import RedirectResponse
from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.database import get_db
from app.models.download_event import DownloadEvent
from app.models.release import Release, ReleaseAsset
from app.utils.rate_limiter import get_client_ip
from app.utils.security import hash_ip

router = APIRouter(prefix="/api/download", tags=["downloads"])


def normalize_platform(platform: str) -> str:
    p = platform.lower().strip()
    if p in ["windows", "win", "win32", "win64"]:
        return "windows"
    if p in ["macos", "mac", "darwin", "osx"]:
        return "macos"
    if p in ["linux", "ubuntu", "debian", "fedora", "arch"]:
        return "linux"
    return p


def normalize_arch(arch: str) -> str:
    a = arch.lower().strip()
    if a in ["x64", "x86_64", "amd64", "intel"]:
        return "x64"
    if a in ["arm64", "aarch64", "apple-silicon", "m1", "m2", "m3", "m4"]:
        return "arm64"
    return a


@router.api_route("/{platform}/{arch}", methods=["GET", "HEAD"])
async def download_installer(
    platform: str,
    arch: str,
    request: Request,
    kind: Optional[str] = Query(None, description="Packaging kind: exe, msi, dmg, deb, rpm, appimage"),
    db: AsyncSession = Depends(get_db),
):
    """
    Log an anonymized download event (without PII) and 302-redirect to the release asset URL.
    """
    norm_platform = normalize_platform(platform)
    norm_arch = normalize_arch(arch)
    stmt = (
        select(Release)
        .options(selectinload(Release.assets))
        .where(Release.is_published == True)
        .order_by(desc(Release.published_at))
        .limit(1)
    )
    result = await db.execute(stmt)
    release = result.scalars().first()

    if not release or not release.assets:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No downloadable assets available at this time.",
        )
    matching_assets = [
        a
        for a in release.assets
        if a.platform == norm_platform and (a.arch == norm_arch or a.arch == "all")
    ]

    if kind:
        specific = [a for a in matching_assets if a.kind.lower() == kind.lower().strip()]
        target_asset = specific[0] if specific else None
    else:
        target_asset = matching_assets[0] if matching_assets else None

    if not target_asset:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Installer not found for platform '{platform}' and architecture '{arch}'.",
        )
    client_ip = get_client_ip(request)
    ip_hashed = hash_ip(client_ip)
    user_agent = request.headers.get("user-agent", "")[:500]
    referer = request.headers.get("referer", "")[:500]

    if not (
        target_asset.download_url.startswith("https://")
        or target_asset.download_url.startswith("http://")
        or target_asset.download_url.startswith("/")
    ):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Insecure or invalid download URL destination.",
        )

    download_log = DownloadEvent(
        release_asset_id=target_asset.id,
        platform=norm_platform,
        arch=norm_arch,
        kind=target_asset.kind,
        version=release.version,
        ip_hash=ip_hashed,
        user_agent=user_agent,
        referer=referer,
    )
    db.add(download_log)
    await db.commit()
    return RedirectResponse(url=target_asset.download_url, status_code=status.HTTP_302_FOUND)
