import pytest
from datetime import datetime, timedelta, timezone
from httpx import AsyncClient, ASGITransport
import jwt
from app.main import app
from app.config import settings
from app.utils.security import create_access_token

@pytest.mark.asyncio
async def test_idor_cross_tenant_join_code_blocked():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        reg_res = await client.post(
            "/api/auth/register-business",
            json={
                "business_name": "Org One",
                "full_name": "Owner One",
                "email": "owner1@tenant.com",
                "password": "Password123!",
            },
        )
        assert reg_res.status_code == 201
        data = reg_res.json()
        token = data["access_token"]
        org1_id = data["organization"]["id"]

        idor_res = await client.post(
            "/api/organizations/9999/join-codes",
            headers={"Authorization": f"Bearer {token}"},
            json={"role": "EMPLOYEE"},
        )
        assert idor_res.status_code == 403
        assert idor_res.json()["error"]["code"] == "FORBIDDEN"

@pytest.mark.asyncio
async def test_tampered_jwt_rejected():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        valid_token = create_access_token({"sub": "1", "org_id": 1, "role_id": 1})
        tampered_token = valid_token[:-4] + "fake"

        res = await client.post(
            "/api/organizations/1/join-codes",
            headers={"Authorization": f"Bearer {tampered_token}"},
            json={"role": "EMPLOYEE"},
        )
        assert res.status_code == 401
        assert res.json()["error"]["code"] == "UNAUTHORIZED"

@pytest.mark.asyncio
async def test_expired_jwt_rejected():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        expired_token = create_access_token(
            {"sub": "1", "org_id": 1, "role_id": 1},
            expires_delta=timedelta(seconds=-10),
        )

        res = await client.post(
            "/api/organizations/1/join-codes",
            headers={"Authorization": f"Bearer {expired_token}"},
            json={"role": "EMPLOYEE"},
        )
        assert res.status_code == 401

@pytest.mark.asyncio
async def test_password_length_restriction():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        oversized_pwd = "A" * 75
        res = await client.post(
            "/api/auth/register-business",
            json={
                "business_name": "Test Org",
                "full_name": "Tester",
                "email": "tester_over@tenant.com",
                "password": oversized_pwd,
            },
        )
        assert res.status_code == 422
        assert res.json()["error"]["code"] == "VALIDATION_ERROR"

@pytest.mark.asyncio
async def test_sql_injection_resilience():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        sqli_email = "' OR '1'='1"
        res = await client.post(
            "/api/auth/login",
            json={"email": sqli_email, "password": "Password123!"},
        )
        assert res.status_code == 422
