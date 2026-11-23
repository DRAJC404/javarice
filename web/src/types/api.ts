export type SupportedPlatform = "windows" | "macos" | "linux";
export type SupportedArch = "x64" | "arm64" | "all";
export type PackagingKind = "exe" | "msi" | "dmg" | "deb" | "rpm" | "appimage";

export interface ReleaseAsset {
  id: number;
  platform: string;
  arch: string;
  kind: string;
  file_name: string;
  download_url: string;
  sha256: string;
  size_bytes: number;
  created_at?: string;
}

export interface Release {
  id: number;
  version: string;
  tag_name: string;
  name: string;
  notes: string;
  is_prerelease: boolean;
  published_at: string;
  assets: ReleaseAsset[];
}

export interface SendLinkRequest {
  email: string;
  platform?: string;
  arch?: string;
  consent?: boolean;
}

export interface LeadResponse {
  message: string;
  email: string;
  success: boolean;
}

export interface HealthResponse {
  status: string;
  service: string;
  environment: string;
}

export interface RegisterBusinessRequest {
  business_name: string;
  full_name: string;
  email: string;
  password: string;
}

export interface UserResponse {
  id: number;
  organization_id: number;
  role_id: number;
  full_name: string;
  email: string;
  approval_status: string;
  is_active: boolean;
  created_at: string;
}

export interface OrgResponse {
  id: number;
  name: string;
  slug: string;
  is_active: boolean;
  created_at: string;
}

export interface AuthResponse {
  access_token: string;
  refresh_token?: string | null;
  token_type: string;
  user: UserResponse;
  organization: OrgResponse;
}

export interface ApiError {
  error: {
    code: string;
    message: string;
    details?: unknown;
  };
}
