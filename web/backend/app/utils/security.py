import hashlib
import re
import secrets
import string
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional, Tuple
import bcrypt
import jwt
from app.config import settings


def hash_password(password: str) -> str:
    """Hash password using BCrypt with 12 rounds, compatible with JavaFX $2a$ format."""
    if not password:
        raise ValueError("Password cannot be empty")
    raw = bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt(rounds=12)).decode("utf-8")
    if raw.startswith("$2b$"):
        raw = "$2a$" + raw[4:]
    return raw


def verify_password(plain_password: str, stored_hash: str) -> bool:
    """Verify password against stored BCrypt hash, handling $2a$/$2b$ compatibility."""
    if not plain_password or not stored_hash:
        return False
    normalized_hash = (
        "$2b$" + stored_hash[4:] if stored_hash.startswith("$2a$") else stored_hash
    )
    try:
        return bcrypt.checkpw(plain_password.encode("utf-8"), normalized_hash.encode("utf-8"))
    except Exception:
        return False


def sha256_hex(raw_text: str) -> str:
    """Compute 64-character lowercase hexadecimal SHA-256 hash."""
    if raw_text is None:
        return ""
    return hashlib.sha256(raw_text.encode("utf-8")).hexdigest().lower()


def normalize_join_code(raw_code: str) -> str:
    """
    Normalizes join code matching Java AuthService:
    removes whitespaces, hyphens, non-breaking spaces, and converts to uppercase.
    """
    if not raw_code:
        return ""
    return re.sub(r"[\s\-\u00A0\u200B\uFEFF]+", "", raw_code).upper().strip()


def generate_join_code() -> str:
    """Generates an 8-character human-readable code in XXXX-XXXX format."""
    alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    part1 = "".join(secrets.choice(alphabet) for _ in range(4))
    part2 = "".join(secrets.choice(alphabet) for _ in range(4))
    return f"{part1}-{part2}"


def create_access_token(data: Dict[str, Any], expires_delta: Optional[timedelta] = None) -> str:
    """Generate signed JWT access token."""
    to_encode = data.copy()
    now = datetime.now(timezone.utc)
    if expires_delta:
        expire = now + expires_delta
    else:
        expire = now + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire, "iat": now})
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)


def decode_access_token(token: str) -> Optional[Dict[str, Any]]:
    """Decode and validate signed JWT access token."""
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        return payload
    except jwt.PyJWTError:
        return None


def generate_remember_token() -> Tuple[str, str, datetime]:
    """
    Generates a secure random 32-byte hex token, its SHA-256 hash (stored in DB),
    and its expiration date (30 days from now).
    """
    raw_token = secrets.token_hex(32)
    token_hash = sha256_hex(raw_token)
    expires_at = datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    return raw_token, token_hash, expires_at


def hash_ip(ip_address: Optional[str]) -> str:
    """Produces a privacy-preserving SHA-256 hash of an IP address without storing raw PII."""
    if not ip_address:
        return sha256_hex("unknown-ip")
    salt = settings.SECRET_KEY[:16]
    return sha256_hex(f"{salt}:{ip_address}")
