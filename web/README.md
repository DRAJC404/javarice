# bAI landing page

A single static page for the bAI desktop app, built with Next.js (App Router), TypeScript and Tailwind CSS. It has no backend and makes no API calls.

## Run locally

```bash
npm install
npm run dev
```

Open http://localhost:3000.

## Build

```bash
npm run build
```

This writes a fully static site to `out/`. Preview it with any static file server, for example:

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
