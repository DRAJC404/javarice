from datetime import datetime
from typing import Optional
from sqlalchemy import DateTime, ForeignKey, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.models.base import Base
from app.models.release import ReleaseAsset


class DownloadEvent(Base):
    __tablename__ = "download_events"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    release_asset_id: Mapped[Optional[int]] = mapped_column(
        Integer, ForeignKey("release_assets.id", ondelete="SET NULL"), nullable=True, index=True
    )
    platform: Mapped[str] = mapped_column(String(30), nullable=False)
    arch: Mapped[Optional[str]] = mapped_column(String(30), nullable=True)
    kind: Mapped[Optional[str]] = mapped_column(String(30), nullable=True)
    version: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    ip_hash: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    user_agent: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    referer: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    downloaded_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    release_asset: Mapped[Optional["ReleaseAsset"]] = relationship("ReleaseAsset")
