"use client";

import { downloadUrl, primaryFile, site } from "@/config/site";
import { osLabel, useDetectedOS } from "@/lib/detectOS";

interface Props {
  size?: "sm" | "lg";
  hideMessage?: boolean;
}

export function DownloadButton({ size = "lg" }: Props) {
  const os = useDetectedOS();
  const sizing = size === "sm" ? "h-10 px-4 text-[14px]" : "h-12 px-6 text-[16px]";

  if (os === null || os === "unknown") {
    return (
      <div className="flex flex-wrap items-center gap-2">
        <a
          href={downloadUrl("linuxDeb")}
          download={site.files.linuxDeb}
          className={`btn-primary ${sizing} inline-flex items-center gap-2`}
        >
          <span>Download for Linux</span>
          <span className="text-xs opacity-75 font-normal">(.deb)</span>
        </a>
        <a
          href="/download"
          className="text-xs font-semibold text-primary hover:underline px-2 py-1"
        >
          All platforms &rarr;
        </a>
      </div>
    );
  }

  const fileKey = primaryFile[os];
  const fileName = site.files[fileKey];
  const extension = fileName.substring(fileName.lastIndexOf("."));

  return (
    <div className="flex flex-wrap items-center gap-3">
      <a
        href={downloadUrl(fileKey)}
        download={fileName}
        className={`btn-primary ${sizing} inline-flex items-center gap-2 shadow-md hover:shadow-lg transition-all`}
      >
        <span>Download for {osLabel[os]}</span>
        <span className="rounded bg-white/20 px-1.5 py-0.5 text-xs font-mono font-normal">
          {extension}
        </span>
      </a>
    </div>
  );
}
