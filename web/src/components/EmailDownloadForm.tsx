"use client";

import { useState, type FormEvent } from "react";
import { sendDownloadLink } from "@/lib/api";

interface Props {
  initialPlatform?: string;
  className?: string;
}

export function EmailDownloadForm({ initialPlatform = "windows", className = "" }: Props) {
  const [email, setEmail] = useState("");
  const [platform, setPlatform] = useState(initialPlatform);
  const [consent, setConsent] = useState(true);
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [message, setMessage] = useState("");

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!email || !email.includes("@")) {
      setStatus("error");
      setMessage("Please enter a valid email address.");
      return;
    }

    setStatus("loading");
    setMessage("");

    try {
      const res = await sendDownloadLink({
        email,
        platform,
        consent,
      });
      setStatus("success");
      setMessage(res.message || "Download link sent! Check your inbox.");
      setEmail("");
    } catch (err) {
      setStatus("error");
      setMessage(err instanceof Error ? err.message : "Something went wrong. Please try again.");
    }
  }

  return (
    <div className={`rounded-xl border border-line bg-white p-6 shadow-sm ${className}`}>
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-navy">Get bAI for Desktop</h3>
        <p className="mt-1 text-sm text-muted">
          On mobile right now? Enter your email to send installer links directly to your inbox.
        </p>
      </div>

      {status === "success" ? (
        <div className="rounded-lg bg-green-50 p-4 border border-green-200 text-green-800">
          <p className="font-medium">Check your inbox!</p>
          <p className="mt-1 text-sm">{message}</p>
          <button
            type="button"
            onClick={() => setStatus("idle")}
            className="mt-3 text-xs font-semibold text-green-700 underline"
          >
            Send to another email
          </button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="lead-email" className="block text-xs font-medium text-navy uppercase tracking-wider mb-1">
              Email Address
            </label>
            <input
              id="lead-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
              required
              disabled={status === "loading"}
              className="w-full rounded-lg border border-line bg-tint px-3 py-2 text-sm text-navy placeholder:text-muted focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="lead-platform" className="block text-xs font-medium text-navy uppercase tracking-wider mb-1">
                Primary OS
              </label>
              <select
                id="lead-platform"
                value={platform}
                onChange={(e) => setPlatform(e.target.value)}
                disabled={status === "loading"}
                className="w-full rounded-lg border border-line bg-tint px-3 py-2 text-sm text-navy focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value="windows">Windows</option>
                <option value="macos">macOS</option>
                <option value="linux">Linux</option>
              </select>
            </div>

            <div className="flex items-end">
              <button
                type="submit"
                disabled={status === "loading"}
                className="btn-primary w-full h-[38px] text-sm font-medium disabled:opacity-50"
              >
                {status === "loading" ? "Sending..." : "Send Link"}
              </button>
            </div>
          </div>

          <label className="flex items-center gap-2 cursor-pointer text-xs text-muted">
            <input
              type="checkbox"
              checked={consent}
              onChange={(e) => setConsent(e.target.checked)}
              className="rounded border-line text-primary focus:ring-primary"
            />
            Notify me of future bAI product releases and updates
          </label>

          {status === "error" && (
            <div className="rounded-lg bg-red-50 p-3 border border-red-200 text-xs text-red-700">
              {message}
            </div>
          )}
        </form>
      )}
    </div>
  );
}
