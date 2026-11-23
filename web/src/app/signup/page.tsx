"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { Nav } from "@/components/Nav";
import { Footer } from "@/components/Footer";
import { registerBusiness } from "@/lib/api";
import { AuthResponse } from "@/types/api";

export default function SignUpPage() {
  const [businessName, setBusinessName] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [errorMsg, setErrorMsg] = useState("");
  const [authData, setAuthData] = useState<AuthResponse | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!businessName.trim() || !fullName.trim() || !email.trim() || !password) {
      setErrorMsg("Please fill in all required fields.");
      setStatus("error");
      return;
    }
    if (password.length < 8) {
      setErrorMsg("Password must be at least 8 characters long.");
      setStatus("error");
      return;
    }

    setStatus("loading");
    setErrorMsg("");

    try {
      const data = await registerBusiness({
        business_name: businessName.trim(),
        full_name: fullName.trim(),
        email: email.trim().toLowerCase(),
        password,
      });
      setAuthData(data);
      setStatus("success");
    } catch (err) {
      setStatus("error");
      setErrorMsg(err instanceof Error ? err.message : "Registration failed. Please try again.");
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-[#EEF4FF] via-[#F8FAFF] to-[#F1F5FD] flex flex-col justify-between">
      <Nav />
      <main className="container-page py-12 md:py-20 max-w-xl flex-1">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-semibold tracking-tight text-navy sm:text-4xl">
            Register Your Business
          </h1>
          <p className="mt-3 text-muted text-base">
            Set up your organization tenant account to start managing stock and inviting store staff.
          </p>
        </div>

        {status === "success" && authData ? (
          <div className="rounded-2xl border border-line bg-white p-8 shadow-sm text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-green-100 text-green-600 mb-4 text-2xl font-bold">
              ✓
            </div>
            <h2 className="text-2xl font-semibold text-navy">Welcome, {authData.user.full_name}!</h2>
            <p className="mt-2 text-sm text-muted">
              Your business organization <strong className="text-navy">{authData.organization.name}</strong> is registered and approved.
            </p>

            <div className="my-6 rounded-lg bg-tint p-4 text-left font-mono text-xs text-muted border border-line">
              <p><span className="text-navy font-semibold">Account Email:</span> {authData.user.email}</p>
              <p className="mt-1"><span className="text-navy font-semibold">Organization:</span> {authData.organization.name} ({authData.organization.slug})</p>
              <p className="mt-1"><span className="text-navy font-semibold">Role:</span> Store Owner (Admin)</p>
            </div>

            <div className="space-y-3">
              <Link
                href="/download"
                className="btn-primary inline-flex h-11 w-full items-center justify-center text-sm font-medium"
              >
                Download bAI for Desktop to Log In →
              </Link>
            </div>
          </div>
        ) : (
          <div className="rounded-2xl border border-line bg-white p-8 shadow-sm">
            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label htmlFor="businessName" className="block text-xs font-semibold text-navy uppercase tracking-wider mb-1">
                  Business / Store Name *
                </label>
                <input
                  id="businessName"
                  type="text"
                  value={businessName}
                  onChange={(e) => setBusinessName(e.target.value)}
                  placeholder="e.g. Apex Hardware & Goods"
                  required
                  disabled={status === "loading"}
                  className="w-full rounded-lg border border-line bg-tint px-3.5 py-2.5 text-sm text-navy placeholder:text-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              <div>
                <label htmlFor="fullName" className="block text-xs font-semibold text-navy uppercase tracking-wider mb-1">
                  Owner Full Name *
                </label>
                <input
                  id="fullName"
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="e.g. Alex Mercer"
                  required
                  disabled={status === "loading"}
                  className="w-full rounded-lg border border-line bg-tint px-3.5 py-2.5 text-sm text-navy placeholder:text-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              <div>
                <label htmlFor="email" className="block text-xs font-semibold text-navy uppercase tracking-wider mb-1">
                  Business Email Address *
                </label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="owner@company.com"
                  required
                  disabled={status === "loading"}
                  className="w-full rounded-lg border border-line bg-tint px-3.5 py-2.5 text-sm text-navy placeholder:text-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              <div>
                <label htmlFor="password" className="block text-xs font-semibold text-navy uppercase tracking-wider mb-1">
                  Password (min 8 characters) *
                </label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  disabled={status === "loading"}
                  className="w-full rounded-lg border border-line bg-tint px-3.5 py-2.5 text-sm text-navy placeholder:text-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              {status === "error" && (
                <div className="rounded-lg bg-red-50 p-3 text-xs text-red-700 border border-red-200">
                  {errorMsg}
                </div>
              )}

              <button
                type="submit"
                disabled={status === "loading"}
                className="btn-primary w-full h-11 text-sm font-semibold disabled:opacity-50"
              >
                {status === "loading" ? "Registering Organization..." : "Create Business Account"}
              </button>
            </form>

            <div className="mt-6 text-center text-xs text-muted">
              By registering, you agree to our{" "}
              <Link href="/terms" className="text-primary hover:underline">
                Terms of Service
              </Link>{" "}
              and{" "}
              <Link href="/privacy" className="text-primary hover:underline">
                Privacy Policy
              </Link>.
            </div>
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}
