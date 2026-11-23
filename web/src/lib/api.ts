import {
  AuthResponse,
  HealthResponse,
  LeadResponse,
  RegisterBusinessRequest,
  Release,
  SendLinkRequest,
} from "@/types/api";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ||
  (typeof window !== "undefined" && window.location.hostname === "localhost"
    ? "http://127.0.0.1:8000"
    : "");

export async function fetchLatestRelease(): Promise<Release | null> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/releases/latest`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 60 },
    });
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export async function fetchReleases(): Promise<Release[]> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/releases`, {
      headers: { Accept: "application/json" },
      next: { revalidate: 60 },
    });
    if (!res.ok) return [];
    return await res.json();
  } catch {
    return [];
  }
}

export async function sendDownloadLink(payload: SendLinkRequest): Promise<LeadResponse> {
  const res = await fetch(`${API_BASE_URL}/api/leads/send-link`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify(payload),
  });

  const data = await res.json();
  if (!res.ok) {
    const msg = data?.error?.message || "Failed to send download link. Please try again.";
    throw new Error(msg);
  }

  return data as LeadResponse;
}

export function getDownloadApiUrl(platform: string, arch: string, kind?: string): string {
  const query = kind ? `?kind=${encodeURIComponent(kind)}` : "";
  return `${API_BASE_URL}/api/download/${encodeURIComponent(platform)}/${encodeURIComponent(arch)}${query}`;
}

export async function checkHealth(): Promise<HealthResponse | null> {
  try {
    const res = await fetch(`${API_BASE_URL}/api/health`);
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

export async function registerBusiness(payload: RegisterBusinessRequest): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE_URL}/api/auth/register-business`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify(payload),
  });

  const data = await res.json();
  if (!res.ok) {
    const msg = data?.error?.message || "Registration failed. Please check your inputs.";
    throw new Error(msg);
  }

  return data as AuthResponse;
}
