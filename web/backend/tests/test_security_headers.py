import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_security_headers_present():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/api/health")
        assert response.status_code == 200
        assert response.headers.get("x-content-type-options") == "nosniff"
        assert response.headers.get("x-frame-options") == "DENY"
        assert response.headers.get("x-xss-protection") == "1; mode=block"
        assert response.headers.get("referrer-policy") == "strict-origin-when-cross-origin"
        assert "camera=()" in response.headers.get("permissions-policy", "")

@pytest.mark.asyncio
async def test_oversized_payload_rejected():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        headers = {"content-length": str(15 * 1024 * 1024)}
        response = await client.post("/api/leads/send-link", headers=headers, json={"email": "test@example.com"})
        assert response.status_code == 413
        data = response.json()
        assert data["error"]["code"] == "PAYLOAD_TOO_LARGE"

@pytest.mark.asyncio
async def test_constant_time_admin_key():
    from app.config import settings
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        res = await client.post(
            "/api/admin/releases",
            headers={"X-API-Key": "wrong-key"},
            json={"version": "9.9.9", "tag_name": "v9.9.9", "name": "Fake", "notes": "notes"}
        )
        assert res.status_code == 401
        assert res.json()["error"]["code"] == "UNAUTHORIZED"

        res_valid = await client.post(
            "/api/admin/releases",
            headers={"X-API-Key": settings.ADMIN_API_KEY},
            json={"version": "9.9.9", "tag_name": "v9.9.9", "name": "Fake", "notes": "notes"}
        )
        assert res_valid.status_code == 201
