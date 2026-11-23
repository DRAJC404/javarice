export const site = {
  name: "bAI",
  version: "1.0.0",
  supportEmail: "support@bai-pos.com",
  releasesBase: process.env.NEXT_PUBLIC_RELEASES_BASE || "/downloads",
  files: {
    windowsMsi: "bAI.msi",
    macAppleSilicon: "bAI-arm64.dmg",
    macIntel: "bAI-x64.dmg",
    linuxDeb: "bai_1.0.0_amd64.deb",
    linuxRpm: "bAI.rpm",
    linuxAppImage: "bAI.AppImage",
  },
} as const;

export type FileKey = keyof typeof site.files;

export function downloadUrl(key: FileKey): string {
  return `${site.releasesBase}/${site.files[key]}`;
}

export const primaryFile = {
  windows: "windowsMsi",
  macos: "macAppleSilicon",
  linux: "linuxDeb",
} as const satisfies Record<string, FileKey>;

export const basePath = process.env.NEXT_PUBLIC_BASE_PATH ?? "";
