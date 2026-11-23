from datetime import datetime
from typing import List, Literal, Optional
from pydantic import BaseModel, ConfigDict, Field


class ReleaseAssetResponse(BaseModel):
    id: int
    platform: str
    arch: str
    kind: str
    file_name: str
    download_url: str
    sha256: str
    size_bytes: int
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class ReleaseResponse(BaseModel):
    id: int
    version: str
    tag_name: str
    name: str
    notes: Optional[str] = None
    is_prerelease: bool
    published_at: datetime
    assets: List[ReleaseAssetResponse] = []

    model_config = ConfigDict(from_attributes=True)


class CreateReleaseAssetSchema(BaseModel):
    platform: Literal["windows", "macos", "linux"]
    arch: Literal["x64", "arm64"]
    kind: Literal["exe", "msi", "dmg", "deb", "rpm", "appimage"]
    file_name: str
    download_url: str
    sha256: str = Field(..., min_length=64, max_length=64, description="Hexadecimal SHA-256 checksum")
    size_bytes: int = Field(..., gt=0)


class CreateReleaseRequest(BaseModel):
    version: str = Field(..., description="Semantic version, e.g. 1.0.0")
    tag_name: str = Field(..., description="Git tag, e.g. v1.0.0")
    name: str = Field(..., description="Human-readable release title")
    notes: Optional[str] = Field(None, description="Markdown changelog notes")
    is_prerelease: bool = False
    is_published: bool = True
    assets: List[CreateReleaseAssetSchema] = []
