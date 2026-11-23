from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict, EmailStr, Field


class RegisterBusinessRequest(BaseModel):
    business_name: str = Field(..., min_length=2, max_length=150, description="Business or store name")
    full_name: str = Field(..., min_length=2, max_length=100, description="Owner full name")
    email: EmailStr = Field(..., description="Business owner email address")
    password: str = Field(..., min_length=8, max_length=72, description="Password (8 to 72 characters)")


class LoginRequest(BaseModel):
    email: EmailStr
    password: str
    remember_me: bool = False
    device_id: Optional[str] = None


class JoinRequest(BaseModel):
    code: str = Field(..., description="8-character invite code, e.g. G87M-6MZK")
    full_name: str = Field(..., min_length=2, max_length=100)
    email: EmailStr
    password: str = Field(..., min_length=8, max_length=72, description="Password (8 to 72 characters)")
    device_id: Optional[str] = None


class RefreshTokenRequest(BaseModel):
    refresh_token: str


class OrgResponse(BaseModel):
    id: int
    name: str
    slug: Optional[str] = None
    is_active: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class UserResponse(BaseModel):
    id: int
    organization_id: int
    role_id: int
    full_name: str
    email: str
    approval_status: str
    is_active: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class AuthResponse(BaseModel):
    access_token: str
    refresh_token: Optional[str] = None
    token_type: str = "bearer"
    user: UserResponse
    organization: OrgResponse


class TokenRefreshResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
