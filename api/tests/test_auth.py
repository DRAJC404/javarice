import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_register_business(client: AsyncClient):
    payload = {
        "business_name": "Corner Grocery",
        "full_name": "Alice Smith",
        "email": "alice@cornergrocery.com",
        "password": "SecurePassword123!",
    }
    response = await client.post("/api/auth/register-business", json=payload)
    assert response.status_code == 201, response.text
    data = response.json()
    assert "access_token" in data
    assert data["user"]["email"] == "alice@cornergrocery.com"
    assert data["user"]["role_id"] == 1
    assert data["user"]["approval_status"] == "APPROVED"
    assert data["organization"]["name"] == "Corner Grocery"


@pytest.mark.asyncio
async def test_register_duplicate_email(client: AsyncClient):
    payload = {
        "business_name": "Corner Grocery 2",
        "full_name": "Bob Smith",
        "email": "dup@cornergrocery.com",
        "password": "SecurePassword123!",
    }
    res1 = await client.post("/api/auth/register-business", json=payload)
    assert res1.status_code == 201
    res2 = await client.post("/api/auth/register-business", json=payload)
    assert res2.status_code == 400
    assert "already exists" in res2.json()["error"]["message"]


@pytest.mark.asyncio
async def test_login_flow(client: AsyncClient):
    reg_payload = {
        "business_name": "Downtown Bakery",
        "full_name": "Charlie Baker",
        "email": "charlie@downtownbakery.com",
        "password": "SecretPassword123!",
    }
    await client.post("/api/auth/register-business", json=reg_payload)
    login_payload = {
        "email": "charlie@downtownbakery.com",
        "password": "SecretPassword123!",
        "remember_me": True,
    }
    res = await client.post("/api/auth/login", json=login_payload)
    assert res.status_code == 200
    data = res.json()
    assert "access_token" in data
    assert data["refresh_token"] is not None
    assert data["user"]["full_name"] == "Charlie Baker"
    bad_login = {
        "email": "charlie@downtownbakery.com",
        "password": "WrongPassword!",
    }
    bad_res = await client.post("/api/auth/login", json=bad_login)
    assert bad_res.status_code == 401
    assert "Invalid email or password" in bad_res.json()["error"]["message"]


@pytest.mark.asyncio
async def test_refresh_token(client: AsyncClient):
    reg_payload = {
        "business_name": "Metro Books",
        "full_name": "Diana Prince",
        "email": "diana@metrobooks.com",
        "password": "WonderPassword123!",
    }
    reg_res = await client.post("/api/auth/register-business", json=reg_payload)
    refresh_token = reg_res.json()["refresh_token"]

    ref_res = await client.post("/api/auth/refresh", json={"refresh_token": refresh_token})
    assert ref_res.status_code == 200
    assert "access_token" in ref_res.json()
