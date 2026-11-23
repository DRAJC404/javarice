"use client";

import { useEffect, useState } from "react";
import { Nav } from "@/components/Nav";
import { Footer } from "@/components/Footer";
import { fetchReleases } from "@/lib/api";
import { Release } from "@/types/api";
import { site } from "@/config/site";

export default function ChangelogPage() {
  const [releases, setReleases] = useState<Release[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const data = await fetchReleases();
        setReleases(data);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const fallbackReleases: Release[] = [
    {
      id: 1,
      version: site.version,
      tag_name: `v${site.version}`,
      name: `bAI ${site.version} — Official Release`,
      notes: `### Features & Improvements
- **Real-Time Stock Deductions**: Deductions for damaged, sold, and expired stock are tracked automatically.
- **Predictive Velocity Monitoring**: Automated velocity alerting and par threshold recommendations.
- **Auditing & Reconciliations**: Monospace join codes for frictionless, safe onboarding.
- **Native Packaging**: Bundled embedded OpenJDK 21 for Windows, macOS, and Linux.
- **Multi-tenant Isolation**: Strict tenant isolation and audit logging.`,
      is_prerelease: false,
      published_at: "2026-11-20T12:00:00Z",
      assets: [],
    },
  ];

  const list = releases.length > 0 ? releases : fallbackReleases;

  return (
    <div className="min-h-screen bg-gradient-to-b from-[#F4F7FD] via-[#F8FAFC] to-[#EEF2F9] flex flex-col justify-between">
      <Nav />
      <main className="container-page py-12 md:py-20 flex-1">
        <div className="max-w-3xl">
          <h1 className="text-3xl font-semibold tracking-tight text-navy sm:text-4xl md:text-5xl">
            Changelog & Releases
          </h1>
          <p className="mt-4 text-lg text-muted">
            New features, fixes, and release notes for bAI Desktop and Platform services.
          </p>
        </div>

        <div className="mt-12 max-w-3xl space-y-12">
          {loading ? (
            <div className="py-12 text-center text-muted">Loading changelog...</div>
          ) : (
            list.map((rel) => {
              const dateStr = new Date(rel.published_at).toLocaleDateString("en-US", {
                year: "numeric",
                month: "long",
                day: "numeric",
              });

              return (
                <article key={rel.id || rel.version} className="relative border-l-2 border-line pl-6">
                  <div className="absolute -left-[9px] top-1.5 h-4 w-4 rounded-full border-2 border-primary bg-white" />
                  <div className="flex flex-wrap items-baseline gap-3">
                    <span className="rounded bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
                      v{rel.version}
                    </span>
                    <time className="text-sm text-muted">{dateStr}</time>
                  </div>

                  <h2 className="mt-3 text-2xl font-semibold text-navy">{rel.name}</h2>

                  <div className="prose prose-sm mt-4 text-muted whitespace-pre-line">
                    {rel.notes}
                  </div>

                  <div className="mt-6">
                    <a href="/download" className="text-sm font-medium text-link">
                      Download v{rel.version} installers →
                    </a>
                  </div>
                </article>
              );
            })
          )}
        </div>
      </main>
      <Footer />
    </div>
  );
}
