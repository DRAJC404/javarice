import Link from "next/link";
import { site } from "@/config/site";

export function Footer() {
  return (
    <footer className="border-t border-slate-800 bg-[#060D1A] text-slate-400">
      <div className="container-page flex flex-wrap items-center justify-between gap-x-8 gap-y-4 py-8 text-[14px]">
        <div>
          <span className="font-semibold text-white">{site.name}</span>
          <span className="ml-4 text-slate-500">Version {site.version}</span>
        </div>

        <div className="flex flex-wrap items-center gap-6 text-slate-400">
          <Link href="/download" className="hover:text-white transition-colors">
            Downloads
          </Link>
          <Link href="/changelog" className="hover:text-white transition-colors">
            Changelog
          </Link>
          <Link href="/signup" className="hover:text-white transition-colors">
            Register
          </Link>
          <Link href="/privacy" className="hover:text-white transition-colors">
            Privacy Policy
          </Link>
          <Link href="/terms" className="hover:text-white transition-colors">
            Terms
          </Link>
          <a href={`mailto:${site.supportEmail}`} className="text-blue-400 hover:text-blue-300">
            {site.supportEmail}
          </a>
        </div>
      </div>
    </footer>
  );
}
