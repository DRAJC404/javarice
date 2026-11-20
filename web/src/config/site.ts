


export const site = {
  name: "bAI",
  version: "1.0.0",
  supportEmail: "support@example.com",

  
  releasesBase: "https://github.com/your-org/bai/releases/download/v1.0.0",

  files: {
    windowsMsi: "bAI.msi",
    macAppleSilicon: "bAI-arm64.dmg",
    macIntel: "bAI-x64.dmg",
    linuxDeb: "bAI.deb",
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
