import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_send_lead_link(client: AsyncClient):
    payload = {
        "email": "mobile.visitor@example.com",
        "platform": "ios",
        "arch": "arm64",
        "consent": True,
    }
    res = await client.post("/api/leads/send-link", json=payload)
    assert res.status_code == 201
    data = res.json()
    assert data["success"] is True
    assert data["email"] == "mobile.visitor@example.com"
