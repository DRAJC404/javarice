"""Add releases, release_assets, download_events, and leads tables

Revision ID: 0002_releases_leads
Revises: 0001_baseline
Create Date: 2026-11-20 00:01:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa
revision: str = '0002_releases_leads'
down_revision: Union[str, None] = '0001_baseline'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        'releases',
        sa.Column('id', sa.Integer(), autoincrement=True, nullable=False),
        sa.Column('version', sa.String(length=50), nullable=False),
        sa.Column('tag_name', sa.String(length=50), nullable=False),
        sa.Column('name', sa.String(length=200), nullable=False),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('is_prerelease', sa.Boolean(), server_default=sa.text('false'), nullable=False),
        sa.Column('is_published', sa.Boolean(), server_default=sa.text('true'), nullable=False),
        sa.Column('published_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index(op.f('ix_releases_version'), 'releases', ['version'], unique=True)
    op.create_index(op.f('ix_releases_tag_name'), 'releases', ['tag_name'], unique=True)
    op.create_table(
        'release_assets',
        sa.Column('id', sa.Integer(), autoincrement=True, nullable=False),
        sa.Column('release_id', sa.Integer(), nullable=False),
        sa.Column('platform', sa.String(length=30), nullable=False),
        sa.Column('arch', sa.String(length=30), nullable=False),
        sa.Column('kind', sa.String(length=30), nullable=False),
        sa.Column('file_name', sa.String(length=255), nullable=False),
        sa.Column('download_url', sa.String(length=1000), nullable=False),
        sa.Column('sha256', sa.String(length=64), nullable=False),
        sa.Column('size_bytes', sa.BigInteger(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['release_id'], ['releases.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index(op.f('ix_release_assets_release_id'), 'release_assets', ['release_id'], unique=False)
    op.create_index(op.f('ix_release_assets_platform'), 'release_assets', ['platform'], unique=False)
    op.create_index(op.f('ix_release_assets_arch'), 'release_assets', ['arch'], unique=False)
    op.create_index(op.f('ix_release_assets_kind'), 'release_assets', ['kind'], unique=False)
    op.create_index('idx_release_assets_lookup', 'release_assets', ['platform', 'arch', 'kind'], unique=False)
    op.create_table(
        'download_events',
        sa.Column('id', sa.Integer(), autoincrement=True, nullable=False),
        sa.Column('release_asset_id', sa.Integer(), nullable=True),
        sa.Column('platform', sa.String(length=30), nullable=False),
        sa.Column('arch', sa.String(length=30), nullable=True),
        sa.Column('kind', sa.String(length=30), nullable=True),
        sa.Column('version', sa.String(length=50), nullable=True),
        sa.Column('ip_hash', sa.String(length=64), nullable=False),
        sa.Column('user_agent', sa.Text(), nullable=True),
        sa.Column('referer', sa.Text(), nullable=True),
        sa.Column('downloaded_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.ForeignKeyConstraint(['release_asset_id'], ['release_assets.id'], ondelete='SET NULL'),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index(op.f('ix_download_events_ip_hash'), 'download_events', ['ip_hash'], unique=False)
    op.create_index(op.f('ix_download_events_release_asset_id'), 'download_events', ['release_asset_id'], unique=False)
    op.create_table(
        'leads',
        sa.Column('id', sa.Integer(), autoincrement=True, nullable=False),
        sa.Column('email', sa.String(length=255), nullable=False),
        sa.Column('platform', sa.String(length=30), nullable=True),
        sa.Column('arch', sa.String(length=30), nullable=True),
        sa.Column('consent', sa.Boolean(), server_default=sa.text('true'), nullable=False),
        sa.Column('ip_hash', sa.String(length=64), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index(op.f('ix_leads_email'), 'leads', ['email'], unique=False)
    op.create_index(op.f('ix_leads_ip_hash'), 'leads', ['ip_hash'], unique=False)


def downgrade() -> None:
    op.drop_table('leads')
    op.drop_table('download_events')
    op.drop_table('release_assets')
    op.drop_table('releases')
