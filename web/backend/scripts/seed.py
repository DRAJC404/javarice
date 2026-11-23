import asyncio
import hashlib
from datetime import datetime, timezone
from sqlalchemy import select
from app.database import AsyncSessionLocal, engine, set_tenant_context
from app.models.organization import Organization
from app.models.release import Release, ReleaseAsset
from app.models.role import Role
from app.models.user import User
from app.utils.security import hash_password


def mock_sha256(seed: str) -> str:
    return hashlib.sha256(seed.encode("utf-8")).hexdigest()


async def seed():
    print("[Seed] Initializing seed data for bAI...")
    async with AsyncSessionLocal() as session:
        roles_data = [
            (1, "OWNER", "Business Owner / Administrator"),
            (2, "MANAGER", "Inventory Manager"),
            (3, "EMPLOYEE", "Store Associate"),
        ]
        for role_id, name, desc in roles_data:
            res = await session.execute(select(Role).where(Role.id == role_id))
            if not res.scalars().first():
                r = Role(id=role_id, name=name, description=desc)
                session.add(r)
        await session.commit()
        print("[Seed] Roles verified.")
        org_check = await session.execute(select(Organization).where(Organization.slug == "apex-retail"))
        org = org_check.scalars().first()
        if not org:
            org = Organization(name="Apex Retail Corp", slug="apex-retail", is_active=True)
            session.add(org)
            await session.flush()
            await set_tenant_context(session, org.id)

            owner_user = User(
                organization_id=org.id,
                role_id=1,
                full_name="Alex Mercer",
                email="owner@apexretail.com",
                password_hash=hash_password("Password123!"),
                approval_status="APPROVED",
                is_active=True,
            )
            session.add(owner_user)
            await session.commit()
            print("[Seed] Demo organization & owner created: owner@apexretail.com / Password123!")
        rel_check = await session.execute(select(Release).where(Release.version == "1.0.0"))
        rel = rel_check.scalars().first()
        if not rel:
            rel = Release(
                version="1.0.0",
                tag_name="v1.0.0",
                name="bAI 1.0.0 — The AI-Powered Inventory System",
                notes="""### Initial General Availability Release
- **Real-Time Stock Deductions**: Automated stock count tracking for damaged, sold, and expired items.
- **Predictive Velocity Monitoring**: Detect high-velocity items and compute dynamic par thresholds.
- **Auditing & Reconciliations**: Fast shift reconciliations with monospace join codes and multi-store spot checks.
- **Native Packaging**: Bundled with embedded OpenJDK runtime; zero external dependencies required.
- **Multi-tenant Isolation**: Strict tenant boundary isolation and audit trail logging.
""",
                is_prerelease=False,
                is_published=True,
                published_at=datetime.now(timezone.utc),
            )
            session.add(rel)
            await session.flush()
            assets_data = [
                {
                    "platform": "windows",
                    "arch": "x64",
                    "kind": "exe",
                    "file_name": "bAI-Setup-1.0.0.exe",
                    "download_url": "https://github.com/bai-inventory/bai/releases/download/v1.0.0/bAI-Setup-1.0.0.exe",
                    "sha256": mock_sha256("bAI-Setup-1.0.0.exe-windows-x64"),
                    "size_bytes": 78_643_200,
                },
                {
                    "platform": "windows",
                    "arch": "x64",
                    "kind": "msi",
                    "file_name": "bAI-1.0.0.msi",
                    "download_url": "https://github.com/bai-inventory/bai/releases/download/v1.0.0/bAI-1.0.0.msi",
                    "sha256": mock_sha256("bAI-1.0.0.msi-windows-x64"),
                    "size_bytes": 79_120_000,
                },
                {
                    "platform": "macos",
                    "arch": "arm64",
                    "kind": "dmg",
                    "file_name": "bAI-1.0.0-arm64.dmg",
                    "download_url": "https://github.com/bai-inventory/bai/releases/download/v1.0.0/bAI-1.0.0-arm64.dmg",
                    "sha256": mock_sha256("bAI-1.0.0-arm64.dmg-macos-arm64"),
                    "size_bytes": 82_450_000,
                },
                {
                    "platform": "macos",
                    "arch": "x64",
                    "kind": "dmg",
                    "file_name": "bAI-1.0.0-x64.dmg",
                    "download_url": "https://github.com/bai-inventory/bai/releases/download/v1.0.0/bAI-1.0.0-x64.dmg",
                    "sha256": mock_sha256("bAI-1.0.0-x64.dmg-macos-x64"),
                    "size_bytes": 84_100_000,
                },
                {
                    "platform": "linux",
                    "arch": "x64",
                    "kind": "deb",
                    "file_name": "bai_1.0.0_amd64.deb",
                    "download_url": "https://github.com/bai-inventory/bai/releases/download/v1.0.0/bai_1.0.0_amd64.deb",
                    "sha256": mock_sha256("bai_1.0.0_amd64.deb-linux-x64"),
                    "size_bytes": 73_200_000,
                },
                {
                    "platform": "linux",
                    "arch": "x64",
                    "kind": "rpm",
                    "file_name": "bai-1.0.0.x86_64.rpm",
                    "download_url": "https://github.com/bai-inventory/bai/releases/download/v1.0.0/bai-1.0.0.x86_64.rpm",
                    "sha256": mock_sha256("bai-1.0.0.x86_64.rpm-linux-x64"),
                    "size_bytes": 74_500_000,
                },
                {
                    "platform": "linux",
                    "arch": "x64",
                    "kind": "appimage",
                    "file_name": "bAI-1.0.0.AppImage",
                    "download_url": "https://github.com/bai-inventory/bai/releases/download/v1.0.0/bAI-1.0.0.AppImage",
                    "sha256": mock_sha256("bAI-1.0.0.AppImage-linux-x64"),
                    "size_bytes": 76_800_000,
                },
            ]

            for a in assets_data:
                asset_obj = ReleaseAsset(
                    release_id=rel.id,
                    platform=a["platform"],
                    arch=a["arch"],
                    kind=a["kind"],
                    file_name=a["file_name"],
                    download_url=a["download_url"],
                    sha256=a["sha256"],
                    size_bytes=a["size_bytes"],
                )
                session.add(asset_obj)

            await session.commit()
            print("[Seed] Seeded release v1.0.0 with 7 installer assets.")
        else:
            print("[Seed] Release v1.0.0 already exists.")

    print("[Seed] Database seeding completed successfully.")


if __name__ == "__main__":
    asyncio.run(seed())
