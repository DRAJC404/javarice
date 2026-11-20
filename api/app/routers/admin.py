from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.database import get_db
from app.models.release import Release, ReleaseAsset
from app.schemas.release import CreateReleaseRequest, ReleaseResponse
from app.utils.dependencies import verify_admin_api_key

router = APIRouter(prefix="/api/admin", tags=["admin"])


@router.post(
    "/releases",
    response_model=ReleaseResponse,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(verify_admin_api_key)],
)
async def publish_or_update_release(
    payload: CreateReleaseRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    CI / Automation endpoint: registers a new application release and attaches
    installer assets with pre-calculated SHA-256 checksums and file sizes.
    Protected by X-API-Key or Bearer token header.
    """
    stmt = (
        select(Release)
        .options(selectinload(Release.assets))
        .where((Release.version == payload.version) | (Release.tag_name == payload.tag_name))
    )
    result = await db.execute(stmt)
    existing_release = result.scalars().first()

    if existing_release:
        existing_release.name = payload.name
        existing_release.notes = payload.notes
        existing_release.is_prerelease = payload.is_prerelease
        existing_release.is_published = payload.is_published
        existing_assets_map = {
            (a.platform, a.arch, a.kind): a for a in existing_release.assets
        }

        for asset_data in payload.assets:
            key = (asset_data.platform, asset_data.arch, asset_data.kind)
            if key in existing_assets_map:
                target = existing_assets_map[key]
                target.file_name = asset_data.file_name
                target.download_url = asset_data.download_url
                target.sha256 = asset_data.sha256
                target.size_bytes = asset_data.size_bytes
            else:
                new_asset = ReleaseAsset(
                    release_id=existing_release.id,
                    platform=asset_data.platform,
                    arch=asset_data.arch,
                    kind=asset_data.kind,
                    file_name=asset_data.file_name,
                    download_url=asset_data.download_url,
                    sha256=asset_data.sha256,
                    size_bytes=asset_data.size_bytes,
                )
                db.add(new_asset)

        await db.commit()
        await db.refresh(existing_release)
        refreshed = await db.execute(
            select(Release).options(selectinload(Release.assets)).where(Release.id == existing_release.id)
        )
        return refreshed.scalars().first()
    new_release = Release(
        version=payload.version,
        tag_name=payload.tag_name,
        name=payload.name,
        notes=payload.notes,
        is_prerelease=payload.is_prerelease,
        is_published=payload.is_published,
    )
    db.add(new_release)
    await db.flush()

    for asset_data in payload.assets:
        asset = ReleaseAsset(
            release_id=new_release.id,
            platform=asset_data.platform,
            arch=asset_data.arch,
            kind=asset_data.kind,
            file_name=asset_data.file_name,
            download_url=asset_data.download_url,
            sha256=asset_data.sha256,
            size_bytes=asset_data.size_bytes,
        )
        db.add(asset)

    await db.commit()
    await db.refresh(new_release)

    refreshed = await db.execute(
        select(Release).options(selectinload(Release.assets)).where(Release.id == new_release.id)
    )
    return refreshed.scalars().first()
