from datetime import datetime
from typing import Literal
from pydantic import BaseModel, ConfigDict, Field


class CreateJoinCodeRequest(BaseModel):
    role: Literal["EMPLOYEE", "MANAGER"] = Field("EMPLOYEE", description="Role to assign upon redemption")


class JoinCodeResponse(BaseModel):
    id: int
    organization_id: int
    code: str
    role: str
    expires_at: datetime
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
