from typing import Optional
from pydantic import BaseModel, EmailStr, Field


class SendLinkRequest(BaseModel):
    email: EmailStr = Field(..., description="Recipient email address")
    platform: Optional[str] = Field(None, description="Visitor OS platform (windows, macos, linux, etc.)")
    arch: Optional[str] = Field(None, description="Visitor architecture (x64, arm64)")
    consent: bool = Field(True, description="Marketing / download link communication consent")


class LeadResponse(BaseModel):
    message: str
    email: str
    success: bool = True
