import { useEffect, useState } from "react";

export type OS = "windows" | "macos" | "linux" | "unknown";

export const osLabel: Record<Exclude<OS, "unknown">, string> = {
  windows: "Windows",
  macos: "macOS",
  linux: "Linux",
};


export function detectOS(): OS {
  if (typeof navigator === "undefined") return "unknown";
  const ua = navigator.userAgent.toLowerCase();
  
  const isIPad = ua.includes("macintosh") && navigator.maxTouchPoints > 1;
  const isMobile = /android|iphone|ipad|ipod|mobile|silk|kindle|blackberry|iemobile|opera mini/.test(ua);
  if (isIPad || isMobile) return "unknown";
  if (ua.includes("windows")) return "windows";
  if (ua.includes("macintosh") || ua.includes("mac os x")) return "macos";
  if (ua.includes("linux") || ua.includes("x11")) return "linux";
  return "unknown";
}


export function useDetectedOS(): OS | null {
  const [os, setOS] = useState<OS | null>(null);
  useEffect(() => setOS(detectOS()), []);
  return os;
}
