# bAI Web Distribution Portal & FastAPI Backend

Next.js 14 App Router (TypeScript, Tailwind CSS) web portal and distribution site for the bAI retail and inventory desktop application, backed by a FastAPI async distribution service.

## Architecture

- **Frontend (`src/`)**: Next.js 14 App Router, TypeScript (strict), Tailwind CSS.
  - `/`: Main landing page with responsive OS detection, feature showcase, and install instructions.
  - `/download`: Dedicated multi-platform download hub with SHA-256 verification and file sizes.
  - `/changelog`: Release notes and version history.
  - Interactive `EmailDownloadForm`: Captures visitor emails on mobile or desktop to send installer links.
- **Backend (`backend/`)**: FastAPI async REST API.
  - `GET /api/health`: Health status check.
  - `GET /api/releases/latest`: Latest published installer release and assets.
  - `GET /api/releases`: Release history and changelog.
  - `GET /api/download/{platform}/{arch}`: 302 redirect to binary asset + anonymized download telemetry.
  - `POST /api/leads/send-link`: Captures email leads with rate-limiting and delivers download links.
  - `POST /api/auth/*`: Organization registration, login, and staff join codes.

## Quickstart with Docker Compose (Recommended)

Run both the frontend web portal and FastAPI backend with a single command:

```bash
docker compose up --build
```

* **Web Portal (Next.js & Nginx)**: [http://localhost:3000](http://localhost:3000)
* **Download Hub**: [http://localhost:3000/download](http://localhost:3000/download)
* **Changelog**: [http://localhost:3000/changelog](http://localhost:3000/changelog)
* **FastAPI Docs (Swagger)**: [http://localhost:8000/docs](http://localhost:8000/docs)
* **API Health Check**: [http://localhost:8000/api/health](http://localhost:8000/api/health)

Nginx automatically routes `/api/*` to the FastAPI backend container internally, eliminating any cross-origin (CORS) friction.

---

## Local Development (Without Docker)

```bash
# Start FastAPI backend (runs on http://localhost:8000)
npm run backend:dev
```
API Documentation is available interactively at [http://localhost:8000/docs](http://localhost:8000/docs).

To run backend tests:
```bash
npm run backend:test
```

### 2. Run the Next.js Frontend

In another terminal:
```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Build Frontend

```bash
npm run build
```

This writes a fully static export to `out/`. Preview it with any static file server:

```bash
npx serve out
```

Don't run `npm run build` while `npm run dev` is running. Both write to `.next/`, and the dev server breaks. Stop the dev server first, or restart it afterwards.

## Before a release

Edit `src/config/site.ts`:

- `version`: shown in the footer
- `supportEmail`: shown in the footer
- `releasesBase`: the GitHub Releases download URL for the tag, e.g. `https://github.com/<owner>/<repo>/releases/download/v1.0.0`
- `files`: the exact asset file names attached to that release

Screenshots live in `public/screens/` (`hero.png` 2400×1500, `stock.png`, `alerts.png`, `audits.png` 1600×1200). Replace them with files of the same names.

## Deploy to Vercel

1. Import the repository in Vercel.
2. Set the root directory to `web`.
3. Keep the detected Next.js preset. Vercel runs `npm run build` and serves the static export.

## Deploy to GitHub Pages

A project site is served from `https://<owner>.github.io/<repo>/`, so build with a base path:

```bash
NEXT_PUBLIC_BASE_PATH=/<repo> npm run build
touch out/.nojekyll
```

Then publish the `out/` folder, either by pushing it to a `gh-pages` branch or with the `actions/upload-pages-artifact` and `actions/deploy-pages` actions. For a user site (`<owner>.github.io`) or a custom domain, leave `NEXT_PUBLIC_BASE_PATH` unset.
