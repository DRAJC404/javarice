import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_update_check_available(client: AsyncClient):
    res = await client.get("/api/updates/check?version=0.8.0&platform=windows&arch=x64")
    assert res.status_code == 200
    data = res.json()
    assert data["update_available"] is True
    assert data["latest_version"] == "1.0.0"
    assert data["download_url"] is not None


@pytest.mark.asyncio
async def test_update_check_current(client: AsyncClient):
    res = await client.get("/api/updates/check?version=1.0.0&platform=windows&arch=x64")
    assert res.status_code == 200
    data = res.json()
    assert data["update_available"] is False
