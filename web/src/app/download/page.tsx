"use client";

import { useEffect, useState } from "react";
import { Nav } from "@/components/Nav";
import { Footer } from "@/components/Footer";
import { EmailDownloadForm } from "@/components/EmailDownloadForm";
import { fetchLatestRelease, getDownloadApiUrl } from "@/lib/api";
import { Release, ReleaseAsset } from "@/types/api";
import { downloadUrl, site } from "@/config/site";

export default function DownloadPage() {
  const [release, setRelease] = useState<Release | null>(null);
  const [loading, setLoading] = useState(true);
  const [copiedSha, setCopiedSha] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const rel = await fetchLatestRelease();
        setRelease(rel);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  function formatBytes(bytes?: number): string {
    if (!bytes) return "—";
    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(1)} MB`;
  }

  function copyText(text: string) {
    navigator.clipboard.writeText(text);
    setCopiedSha(text);
    setTimeout(() => setCopiedSha(null), 2000);
  }

  const staticFallbackAssets: ReleaseAsset[] = [
    {
      id: 1,
      platform: "windows",
      arch: "x64",
      kind: "msi",
      file_name: site.files.windowsMsi,
      download_url: downloadUrl("windowsMsi"),
      sha256: "5a1a708569ebc1e81feb0f3d88936b8c3ce315743e07066b6f44d4d0e38db642",
      size_bytes: 79120000,
    },
    {
      id: 2,
      platform: "macos",
      arch: "arm64",
      kind: "dmg",
      file_name: site.files.macAppleSilicon,
      download_url: downloadUrl("macAppleSilicon"),
      sha256: "a82c1397efe00f71d1a1889d0ef4045769592d25f7835e47096cea1a577d7dc2",
      size_bytes: 82450000,
    },
    {
      id: 3,
      platform: "macos",
      arch: "x64",
      kind: "dmg",
      file_name: site.files.macIntel,
      download_url: downloadUrl("macIntel"),
      sha256: "b5583be02dfc71850a34c063cc584749085de3d1cf1497327078ea47f7abe706",
      size_bytes: 84100000,
    },
    {
      id: 4,
      platform: "linux",
      arch: "x64",
      kind: "deb",
      file_name: site.files.linuxDeb,
      download_url: downloadUrl("linuxDeb"),
      sha256: "fcd9fdc627053b8de594ec6dc396294e5421c1c1d7e6121a13a85a312c7d32db",
      size_bytes: 73200000,
    },
    {
      id: 5,
      platform: "linux",
      arch: "x64",
      kind: "rpm",
      file_name: site.files.linuxRpm,
      download_url: downloadUrl("linuxRpm"),
      sha256: "65e019599055984b43da203cdbaef05319b8c00a76c64240560743f8fa296d47",
      size_bytes: 74500000,
    },
    {
      id: 6,
      platform: "linux",
      arch: "x64",
      kind: "appimage",
      file_name: site.files.linuxAppImage,
      download_url: downloadUrl("linuxAppImage"),
      sha256: "179bcac414625ad000ed881bf435900a4297c1d51e12610c95f827c461e96cb2",
      size_bytes: 76800000,
    },
  ];

  const assets = release?.assets?.length ? release.assets : staticFallbackAssets;
  const currentVersion = release?.version || site.version;

  return (
    <div className="min-h-screen bg-gradient-to-b from-[#EEF4FF] via-[#F8FAFF] to-[#F1F5FD] flex flex-col justify-between">
      <Nav />
      <main className="container-page py-12 md:py-20 flex-1">
        <div className="max-w-3xl">
          <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-xs font-semibold text-primary mb-4">
            <span>Official Release v{currentVersion}</span>
            {loading && <span className="animate-pulse">• checking live updates...</span>}
          </div>
          <h1 className="text-3xl font-semibold tracking-tight text-navy sm:text-4xl md:text-5xl">
            Download bAI Desktop
          </h1>
          <p className="mt-4 text-lg text-muted">
            Native, high-performance inventory and retail tracking. Bundled with embedded OpenJDK runtime — zero Java or runtime prerequisites required.
          </p>
        </div>

        <div className="mt-12 grid gap-10 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-6">
            <h2 className="text-xl font-semibold text-navy">All Platforms & Formats</h2>

            <div className="overflow-hidden rounded-xl border border-line bg-white shadow-sm">
              <div className="divide-y divide-line">
                {assets.map((asset) => {
                  return (
                    <div key={`${asset.platform}-${asset.arch}-${asset.kind}`} className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-tint/40 transition-colors">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-navy capitalize">{asset.platform}</span>
                          <span className="rounded bg-tint px-2 py-0.5 text-xs font-mono uppercase text-muted">
                            {asset.kind} • {asset.arch}
                          </span>
                        </div>
                        <p className="mt-1 text-xs font-mono text-muted">{asset.file_name} ({formatBytes(asset.size_bytes)})</p>
                        {asset.sha256 && (
                          <div className="mt-2 flex items-center gap-2 text-[11px] text-muted">
                            <span className="font-mono truncate max-w-[200px] sm:max-w-[280px]">
                              SHA-256: {asset.sha256}
                            </span>
                            <button
                              type="button"
                              onClick={() => copyText(asset.sha256)}
                              className="text-primary hover:underline"
                            >
                              {copiedSha === asset.sha256 ? "Copied!" : "Copy hash"}
                            </button>
                          </div>
                        )}
                      </div>

                      <a
                        href={asset.download_url}
                        download={asset.file_name}
                        className="btn-primary self-start sm:self-auto h-9 px-4 text-xs font-medium whitespace-nowrap inline-flex items-center gap-1.5"
                      >
                        Download .{asset.kind}
                      </a>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="rounded-xl border border-line bg-tint p-6 text-sm text-muted">
              <h3 className="font-semibold text-navy text-base">Verification & Integrity</h3>
              <p className="mt-1">
                You can verify the SHA-256 checksum of your downloaded binary using your terminal:
              </p>
              <pre className="mt-3 rounded-lg bg-navy p-3 font-mono text-xs text-white overflow-x-auto">
                {`# On Linux / macOS:\nshasum -a 256 <downloaded_file>\n\n# On Windows PowerShell:\nGet-FileHash -Algorithm SHA256 .\\<downloaded_file>`}
              </pre>
            </div>
          </div>

          <div>
            <EmailDownloadForm className="sticky top-24" />
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
