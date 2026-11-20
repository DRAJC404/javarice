import time
from collections import defaultdict
from typing import Dict, List
from fastapi import HTTPException, Request, status


class InMemoryRateLimiter:
    """Sliding window rate limiter for endpoints like lead capture and auth attempts."""

    def __init__(self, requests_limit: int, window_seconds: int):
        self.requests_limit = requests_limit
        self.window_seconds = window_seconds
        self.hits: Dict[str, List[float]] = defaultdict(list)

    def check(self, key: str) -> None:
        now = time.time()
        window_start = now - self.window_seconds
        self.hits[key] = [t for t in self.hits[key] if t > window_start]
        
        if len(self.hits[key]) >= self.requests_limit:
            retry_after = int(self.hits[key][0] + self.window_seconds - now) + 1
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"Rate limit exceeded. Please wait {retry_after} seconds.",
                headers={"Retry-After": str(max(1, retry_after))},
            )
        
        self.hits[key].append(now)
leads_rate_limiter = InMemoryRateLimiter(requests_limit=5, window_seconds=600)
auth_rate_limiter = InMemoryRateLimiter(requests_limit=10, window_seconds=60)


def get_client_ip(request: Request) -> str:
    """Extract client IP address accounting for proxies."""
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.client.host if request.client else "127.0.0.1"
