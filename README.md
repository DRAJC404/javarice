# bAI — AI-Powered Retail & Inventory Platform

> **Smarter Stock Tracking, Zero Manual Counting**  
> Native Desktop POS (JavaFX) • Next.js App Router Web Distribution • Async FastAPI Backend • PostgreSQL Multi-Tenant RLS

---

## 1. Monorepo Overview

This repository is organized as a unified monorepo containing the native desktop application, the modern web distribution portal, the asynchronous FastAPI distribution and auth backend, existing SQL migration schemas, and automated multi-platform GitHub Actions build pipelines.

```
bAI/
├── desktop/                # Existing JavaFX desktop app (Java 21, Maven, OpenPDF, HikariCP)
│   ├── src/                # JavaFX source code (com.bai.inventory)
│   ├── pom.xml             # Maven configuration
│   └── mvnw, mvnw.cmd      # Maven wrappers
├── web/                    # Next.js 14 App Router (TypeScript strict, Tailwind CSS, shadcn/ui)
│   ├── src/app/            # App Router pages: /, /download, /signup, /changelog
│   ├── src/components/     # UI components: DownloadButton, MockupWindow, EmailDownloadForm
│   ├── src/types/api.ts    # Generated from FastAPI OpenAPI schema via openapi-typescript
│   ├── Dockerfile          # Multi-stage production container build
│   └── package.json
├── api/                    # FastAPI Backend (Python 3.12+, SQLAlchemy 2.0 async, Alembic, Pydantic v2)
│   ├── app/                # Application code: routers, models, schemas, services, utils
│   ├── alembic/            # Database revisions (0001_baseline, 0002_releases_leads)
│   ├── tests/              # Pytest + HTTPX async test suite
│   ├── scripts/seed.py     # Database seed script for releases and demo tenants
│   ├── openapi.json        # Exported OpenAPI 3.1 schema
│   ├── Dockerfile          # Production container build
│   └── requirements.txt
├── database/               # Existing PostgreSQL database schemas and migrations
│   ├── database.sql        # Canonical database schema & RLS definitions
│   └── migration_*.sql     # Historical migration scripts
├── .github/workflows/
│   └── build-installers.yml # Native Maven + jpackage installer CI/CD on tag v*
├── docker-compose.yml       # Production-ready compose stack (postgres, api, web)
└── README.md
```

---

## 2. Tech Stack

| Layer | Technologies |
|---|---|
| **Frontend** | Next.js 14 (App Router), TypeScript (Strict), Tailwind CSS, shadcn/ui design tokens, Lucide Icons, `next-themes` (Dark/Light mode) |
| **Backend** | FastAPI, Python 3.12+, SQLAlchemy 2.0 Async (`asyncpg`), Alembic migrations, Pydantic v2, `pydantic-settings` |
| **Database** | PostgreSQL 16 (`bai_pos`) with Row-Level Security (RLS) tenant isolation |
| **Desktop App** | JavaFX 21, OpenJDK 21, HikariCP, jBCrypt, OpenPDF, Jackson |
| **Type Safety** | End-to-end type generation: FastAPI OpenAPI → `openapi-typescript` → TypeScript types |
| **Packaging & CI** | GitHub Actions, Maven, `jpackage` (Windows `.exe`/`.msi`, macOS `.dmg`, Linux `.deb`/`.rpm`/`.AppImage`) |

---

## 3. Quickstart with Docker Compose

The easiest way to run the entire stack (PostgreSQL database, FastAPI API, and Next.js Web frontend) is with Docker Compose:

```bash
# 1. Clone repository
git clone https://github.com/bai-inventory/bai.git
cd bai

# 2. Start all services
docker compose up --build
```

Once running:
- **Web Download Portal**: [http://localhost:3000](http://localhost:3000)
- **FastAPI Interactive Docs**: [http://localhost:8000/docs](http://localhost:8000/docs)
- **PostgreSQL Database**: `localhost:5432` (`bai_pos`, user: `postgres`, password: `postgres`)

The database container automatically initializes using the SQL files in `database/`, runs Alembic migrations, and seeds the initial `v1.0.0` installer releases and demo tenant.

---

## 4. Local Development Setup (Manual)

### Prerequisites
- Node.js 20+ and npm 10+
- Python 3.12+
- Java Development Kit (JDK) 21 (Temurin or OpenJDK)
- PostgreSQL 15+ running locally

---

### Step 1: Database Setup
```bash
# Create the PostgreSQL database
createdb -U postgres bai_pos

# Execute the existing canonical database schema
psql -U postgres -d bai_pos -f database/database.sql
```

---

### Step 2: FastAPI Backend Setup
```bash
cd api

# Create and activate Python virtual environment
python3 -m venv .venv
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Run Alembic migrations
alembic upgrade head

# Seed sample release v1.0.0 and demo tenant
python scripts/seed.py

# Run the test suite (12 async tests covering auth, join codes, downloads, updates, leads)
PYTHONPATH=. pytest tests

# Export OpenAPI schema for frontend types
python -c "import json; from app.main import app; json.dump(app.openapi(), open('openapi.json', 'w'), indent=2)"

# Start the API development server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

---

### Step 3: Next.js Frontend Setup
```bash
cd web

# Install dependencies
npm install

# Regenerate TypeScript types from OpenAPI schema
npm run generate:types

# Start Next.js development server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

---

### Step 4: JavaFX Desktop App
```bash
cd desktop

# Run the native JavaFX desktop client
./mvnw clean javafx:run
```

---

## 5. Security & Cryptographic Compatibility

### BCrypt Password Hashing (`$2a$` vs `$2b$`)
The existing JavaFX application utilizes `org.mindrot.jbcrypt.BCrypt` configured with 12 log rounds (`$2a$12$...`).
- **Python Hashing**: The FastAPI backend generates passwords formatted with `$2a$12$...` ensuring JavaFX client compatibility.
- **Python Verification**: Password verification in `api/app/utils/security.py` normalizes `$2a$` hashes to `$2b$` for modern runtime engines.

### Staff Invitation Join Codes
- **Code Format**: 8-character uppercase alphanumeric string (`XXXX-XXXX`, e.g., `G87M-6MZK`).
- **Hashing**: SHA-256 lowercase 64-character hexadecimal digest of the normalized code (`hashlib.sha256(cleaned.encode()).hexdigest()`).
- **Expiration Rule**: Codes strictly expire 10 minutes after generation.
- **Rate Limiting**: Monitored via the `join_attempt_rate_limits` table (max 10 attempts per minute per device/IP).

### Multi-Tenant Isolation
All organization-scoped queries execute under session-local tenant configuration:
```sql
SELECT set_config('app.current_organization_id', '1', true);
```
Enforcing PostgreSQL Row Level Security (RLS) on `join_codes`, `audit_log`, and inventory movements.

---

## 6. API Endpoints Reference

| Method | Endpoint | Description | Auth / Security |
|---|---|---|---|
| `GET` | `/api/releases/latest` | Get latest published release with assets | Public |
| `GET` | `/api/releases` | List release changelog history | Public |
| `GET` | `/api/download/{platform}/{arch}?kind=` | 302 redirect to installer file & log anonymized download | Public (No raw PII) |
| `POST` | `/api/leads/send-link` | Submit mobile visitor email to receive download links | Rate limited (5 / 10 min) |
| `POST` | `/api/auth/register-business` | Register organization + OWNER user in 1 atomic transaction | Public |
| `POST` | `/api/auth/login` | Authenticate desktop user, check `approval_status` and `is_active` | Rate limited (10 / min) |
| `POST` | `/api/auth/refresh` | Exchange remember token for a new access token | Public |
| `POST` | `/api/auth/join` | Consume 8-char join code, verify rate limit, write audit log | Rate limited (10 / min) |
| `POST` | `/api/organizations/{id}/join-codes` | Generate a 10-minute join code for staff onboarding | OWNER / ADMIN Only |
| `GET` | `/api/updates/check` | Check for updates against installed desktop version | Public |
| `POST` | `/api/admin/releases` | Register/upsert release and checksums from CI pipeline | API Key (`X-API-Key`) |

---

## 7. Production Deployment Guide

### A. Deploy Next.js Web on Vercel
1. Push this repository to GitHub.
2. In Vercel, click **Add New Project** and select the repository.
3. Set **Root Directory** to `web`.
4. Framework Preset: **Next.js**.
5. Add Environment Variables:
   - `NEXT_PUBLIC_API_URL`: URL of your deployed FastAPI backend (e.g. `https://api.bai-pos.com`).
6. Click **Deploy**.

---

### B. Deploy FastAPI on Fly.io
1. Install the Fly CLI: `curl -L https://fly.io/install.sh | sh`
2. Navigate to `api/`:
   ```bash
   cd api
   fly launch --no-deploy
   ```
3. Set secrets on Fly:
   ```bash
   fly secrets set \
     DATABASE_URL="postgresql+asyncpg://user:pass@host:5432/bai_pos?ssl=require" \
     SECRET_KEY="production-secret-key-32-chars-minimum" \
     ADMIN_API_KEY="production-ci-release-key" \
     CORS_ORIGINS="https://bai-pos.com,https://www.bai-pos.com" \
     ENVIRONMENT="production"
   ```
4. Deploy:
   ```bash
   fly deploy
   ```

---

### C. Deploy FastAPI on Railway / Render
1. **New Web Service**: Connect your GitHub repository.
2. **Root Directory**: `api`.
3. **Build Command**: `pip install -r requirements.txt`.
4. **Start Command**:
   ```bash
   alembic upgrade head && python scripts/seed.py && uvicorn app.main:app --host 0.0.0.0 --port $PORT
   ```
5. **Environment Variables**:
   - `DATABASE_URL`: Hosted PostgreSQL connection URL (`postgresql+asyncpg://...`)
   - `SECRET_KEY`: Strong cryptographic random key
   - `ADMIN_API_KEY`: Key matching GitHub Actions secret
   - `CORS_ORIGINS`: Frontend domain(s)

---

## 8. CI/CD Native Installer Release Pipeline

Native installers are built automatically using `.github/workflows/build-installers.yml`:
1. Push a release tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
2. GitHub Actions matrix triggers:
   - `windows-latest`: Builds `bAI-Setup-1.0.1.exe` and `bAI-1.0.1.msi`
   - `macos-latest` (Apple Silicon M-series): Builds `bAI-1.0.1-arm64.dmg`
   - `macos-13` (Intel x86_64): Builds `bAI-1.0.1-x64.dmg`
   - `ubuntu-latest`: Builds `bai_1.0.1_amd64.deb` and `bai-1.0.1.x86_64.rpm`
3. Checksums (`SHA-256`) and binary sizes are computed.
4. Assets are published to the GitHub Release.
5. GitHub Actions calls `POST /api/admin/releases` with `X-API-Key: ${{ secrets.ADMIN_API_KEY }}` to update the live download portal.

---

## 9. Verification & Automated Testing

To run the automated backend test suite:
```bash
cd api
PYTHONPATH=. pytest tests -v
```

Output:
```text
tests/test_auth.py::test_register_business PASSED
tests/test_auth.py::test_register_duplicate_email PASSED
tests/test_auth.py::test_login_flow PASSED
tests/test_auth.py::test_refresh_token PASSED
tests/test_join.py::test_join_code_lifecycle PASSED
tests/test_join.py::test_join_invalid_code PASSED
tests/test_downloads.py::test_get_latest_release PASSED
tests/test_downloads.py::test_download_redirect PASSED
tests/test_downloads.py::test_download_unknown_platform PASSED
tests/test_updates.py::test_update_check_available PASSED
tests/test_updates.py::test_update_check_current PASSED
tests/test_leads.py::test_send_lead_link PASSED

12 passed in 3.69s
```

To build and verify the Next.js web application:
```bash
cd web
npm run build
```

---

## 10. License

Released under the MIT License. Copyright © 2026 bAI Systems, Inc.
