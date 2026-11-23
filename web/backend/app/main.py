from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.config import settings
from app.database import Base, engine
from app.routers import admin, auth, downloads, leads, organizations, releases, updates


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    try:
        from scripts.seed import seed
        await seed()
    except Exception as e:
        print(f"[bAI API] Seed check completed: {e}")
    yield


app = FastAPI(
    title="bAI Inventory Distribution API",
    version="1.0.0",
    description="API for bAI native installers distribution, updates, staff joining, and organization onboarding.",
    lifespan=lifespan,
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
MAX_CONTENT_LENGTH = 10 * 1024 * 1024


@app.middleware("http")
async def security_middleware(request: Request, call_next):
    content_length = request.headers.get("content-length")
    if content_length and int(content_length) > MAX_CONTENT_LENGTH:
        return JSONResponse(
            status_code=status.HTTP_413_CONTENT_TOO_LARGE,
            content={
                "error": {
                    "code": "PAYLOAD_TOO_LARGE",
                    "message": "Request payload exceeds maximum allowed limit (10MB).",
                }
            },
        )

    response = await call_next(request)

    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
    response.headers["Permissions-Policy"] = "camera=(), microphone=(), geolocation=()"
    if settings.ENVIRONMENT != "development":
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
    return response


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    code = "HTTP_ERROR"
    if exc.status_code == 400:
        code = "BAD_REQUEST"
    elif exc.status_code == 401:
        code = "UNAUTHORIZED"
    elif exc.status_code == 403:
        code = "FORBIDDEN"
    elif exc.status_code == 404:
        code = "NOT_FOUND"
    elif exc.status_code == 429:
        code = "RATE_LIMITED"

    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": {
                "code": code,
                "message": exc.detail if isinstance(exc.detail, str) else "An error occurred",
                "details": exc.detail if isinstance(exc.detail, dict) else None,
            }
        },
        headers=exc.headers,
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    first_msg = errors[0]["msg"] if errors else "Validation error"
    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "error": {
                "code": "VALIDATION_ERROR",
                "message": f"Input validation failed: {first_msg}",
                "details": {"validation_errors": errors},
            }
        },
    )
@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": {
                "code": "INTERNAL_SERVER_ERROR",
                "message": "An unexpected server error occurred.",
            }
        },
    )


@app.get("/", tags=["system"])
async def root():
    return {
        "service": "bAI API",
        "status": "online",
        "documentation": "/docs",
        "health_check": "/api/health",
        "latest_release": "/api/releases/latest",
        "web_portal": "http://localhost:3000",
    }


@app.get("/api/health", tags=["system"])
async def health_check():
    return {
        "status": "healthy",
        "service": "bAI API",
        "environment": settings.ENVIRONMENT,
    }
app.include_router(releases.router)
app.include_router(downloads.router)
app.include_router(leads.router)
app.include_router(auth.router)
app.include_router(organizations.router)
app.include_router(updates.router)
app.include_router(admin.router)
