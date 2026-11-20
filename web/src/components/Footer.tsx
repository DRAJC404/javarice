import { site } from "@/config/site";

export function Footer() {
  return (
    <footer>
      <div className="container-page flex flex-wrap items-center justify-between gap-x-8 gap-y-2 py-8 text-[14px] text-muted">
        <p>
          <span className="font-semibold text-navy">{site.name}</span>
          <span className="ml-4">Version {site.version}</span>
        </p>
        <a href={`mailto:${site.supportEmail}`} className="text-link">
          {site.supportEmail}
        </a>
      </div>
    </footer>
  );
}
