import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_join_code_lifecycle(client: AsyncClient):
    reg_payload = {
        "business_name": "Sunrise Hardware",
        "full_name": "Samuel Owner",
        "email": "owner@sunrisehardware.com",
        "password": "Password123!",
    }
    reg_res = await client.post("/api/auth/register-business", json=reg_payload)
    owner_token = reg_res.json()["access_token"]
    org_id = reg_res.json()["organization"]["id"]
    code_res = await client.post(
        f"/api/organizations/{org_id}/join-codes",
        headers={"Authorization": f"Bearer {owner_token}"},
        json={"role": "EMPLOYEE"},
    )
    assert code_res.status_code == 201
    code_data = code_res.json()
    raw_code = code_data["code"]
    assert len(raw_code.replace("-", "")) == 8
    join_payload = {
        "code": raw_code,
        "full_name": "Edward Employee",
        "email": "edward@sunrisehardware.com",
        "password": "EmployeePass123!",
    }
    join_res = await client.post("/api/auth/join", json=join_payload)
    assert join_res.status_code == 201
    staff_data = join_res.json()
    assert staff_data["user"]["role_id"] == 3
    assert staff_data["user"]["organization_id"] == org_id
    assert "access_token" in staff_data
    reuse_res = await client.post(
        "/api/auth/join",
        json={
            "code": raw_code,
            "full_name": "Frank Employee",
            "email": "frank@sunrisehardware.com",
            "password": "EmployeePass123!",
        },
    )
    assert reuse_res.status_code == 400
    assert "already been used" in reuse_res.json()["error"]["message"]


@pytest.mark.asyncio
async def test_join_invalid_code(client: AsyncClient):
    res = await client.post(
        "/api/auth/join",
        json={
            "code": "INVALID8",
            "full_name": "Tester",
            "email": "tester@test.com",
            "password": "Password123!",
        },
    )
    assert res.status_code == 400
    assert "couldn't find that code" in res.json()["error"]["message"]
