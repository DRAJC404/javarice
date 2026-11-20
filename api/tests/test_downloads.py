import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_get_latest_release(client: AsyncClient):
    res = await client.get("/api/releases/latest")
    assert res.status_code == 200
    data = res.json()
    assert data["version"] == "1.0.0"
    assert len(data["assets"]) >= 3


@pytest.mark.asyncio
async def test_download_redirect(client: AsyncClient):
    res = await client.get("/api/download/windows/x64?kind=exe", follow_redirects=False)
    assert res.status_code == 302
    assert "bAI-Setup-1.0.0.exe" in res.headers["location"]
    res_mac = await client.get("/api/download/macos/arm64", follow_redirects=False)
    assert res_mac.status_code == 302
    assert "bAI-1.0.0-arm64.dmg" in res_mac.headers["location"]
    res_linux = await client.get("/api/download/linux/x64", follow_redirects=False)
    assert res_linux.status_code == 302
    assert "bai_1.0.0_amd64.deb" in res_linux.headers["location"]


@pytest.mark.asyncio
async def test_download_unknown_platform(client: AsyncClient):
    res = await client.get("/api/download/freebsd/mips", follow_redirects=False)
    assert res.status_code == 404
