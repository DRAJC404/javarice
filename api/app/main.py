from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.config import settings
from app.routers import admin, auth, downloads, leads, organizations, releases, updates


@asynccontextmanager
async def lifespan(app: FastAPI):
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
