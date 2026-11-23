import asyncio
from typing import AsyncGenerator
import pytest
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from app.database import Base, get_db
from app.main import app
from app.models.role import Role
from app.models.release import Release, ReleaseAsset
from datetime import datetime, timezone
TEST_DB_URL = "sqlite+aiosqlite:///:memory:"

test_engine = create_async_engine(
    TEST_DB_URL,
    connect_args={"check_same_thread": False},
    future=True,
)

TestSessionLocal = async_sessionmaker(
    bind=test_engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
)


@pytest.fixture(scope="session")
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(autouse=True)
async def setup_test_db():
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)
    async with TestSessionLocal() as session:
        roles = [
            Role(id=1, name="OWNER", description="Business Owner"),
            Role(id=2, name="MANAGER", description="Inventory Manager"),
            Role(id=3, name="EMPLOYEE", description="Store Associate"),
        ]
        session.add_all(roles)
        rel = Release(
            version="1.0.0",
            tag_name="v1.0.0",
            name="bAI 1.0.0 Test Release",
            notes="Initial test release",
            is_prerelease=False,
            is_published=True,
            published_at=datetime.now(timezone.utc),
        )
        session.add(rel)
        await session.flush()

        assets = [
            ReleaseAsset(
                release_id=rel.id,
                platform="windows",
                arch="x64",
                kind="exe",
                file_name="bAI-Setup-1.0.0.exe",
                download_url="https://downloads.bai-pos.com/win/bAI-Setup-1.0.0.exe",
                sha256="e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                size_bytes=78000000,
            ),
            ReleaseAsset(
                release_id=rel.id,
                platform="macos",
                arch="arm64",
                kind="dmg",
                file_name="bAI-1.0.0-arm64.dmg",
                download_url="https://downloads.bai-pos.com/mac/bAI-1.0.0-arm64.dmg",
                sha256="ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
                size_bytes=82000000,
            ),
            ReleaseAsset(
                release_id=rel.id,
                platform="linux",
                arch="x64",
                kind="deb",
                file_name="bai_1.0.0_amd64.deb",
                download_url="https://downloads.bai-pos.com/linux/bai_1.0.0_amd64.deb",
                sha256="4e1243bd22c66e76c2ba9eddc1f91394e57f9f835f8d67b7f833f443cfec5316",
                size_bytes=73000000,
            ),
        ]
        session.add_all(assets)
        await session.commit()

    yield

    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


async def override_get_db() -> AsyncGenerator[AsyncSession, None]:
    async with TestSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()


app.dependency_overrides[get_db] = override_get_db


@pytest.fixture
async def client() -> AsyncGenerator[AsyncClient, None]:
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
