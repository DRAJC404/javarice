from datetime import datetime
from typing import Optional
from pydantic import BaseModel


class UpdateCheckResponse(BaseModel):
    update_available: bool
    current_version: str
    latest_version: str
    download_url: Optional[str] = None
    release_notes: Optional[str] = None
    sha256: Optional[str] = None
    published_at: Optional[datetime] = None
