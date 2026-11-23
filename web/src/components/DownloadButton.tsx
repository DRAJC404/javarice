"use client";

import { downloadUrl, primaryFile } from "@/config/site";
import { osLabel, useDetectedOS } from "@/lib/detectOS";
import { EmailDownloadForm } from "./EmailDownloadForm";

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
    return (
      <div className="w-full max-w-[440px]">
        <EmailDownloadForm />
      </div>
    );
  }

  return (
    <a href={downloadUrl(primaryFile[os])} className={`btn-primary ${sizing}`}>
      Download for {osLabel[os]}
    </a>
  );
}
