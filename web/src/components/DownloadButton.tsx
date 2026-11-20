"use client";

import { downloadUrl, primaryFile } from "@/config/site";
import { osLabel, useDetectedOS } from "@/lib/detectOS";

export const DESKTOP_ONLY_MESSAGE = "bAI is a desktop app. Open this page on your computer to download.";

interface Props {
  size?: "sm" | "lg";
  
  hideMessage?: boolean;
}

export function DownloadButton({ size = "lg", hideMessage = false }: Props) {
  const os = useDetectedOS();
  const sizing = size === "sm" ? "h-10 px-4 text-[15px]" : "h-12 px-6 text-[16px]";

  
  if (os === null) {
    return (
      <a href="#install" className={`btn-primary ${sizing}`}>
        Download
      </a>
    );
  }

  if (os === "unknown") {
    if (hideMessage) return null;
    return <p className="max-w-[420px] text-[16px] leading-[1.6] text-muted">{DESKTOP_ONLY_MESSAGE}</p>;
  }

  return (
    <a href={downloadUrl(primaryFile[os])} className={`btn-primary ${sizing}`}>
      Download for {osLabel[os]}
    </a>
  );
}
