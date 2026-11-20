import { DownloadButton } from "./DownloadButton";

const links = [
  { href: "#about", label: "About" },
  { href: "#install", label: "Install" },
];

export function Nav() {
  return (
    <header className="sticky top-0 z-40 border-b border-line bg-white">
      <nav aria-label="Main" className="container-page flex h-16 items-center justify-between gap-6">
        <a href="#top" className="rounded-[6px] text-[20px] font-semibold tracking-[-0.02em] text-navy">
          bAI
        </a>
        <div className="flex items-center gap-4 sm:gap-8">
          <ul className="flex items-center gap-4 sm:gap-6">
            {links.map((l) => (
              <li key={l.href}>
                <a href={l.href} className="nav-link text-[15px] font-medium">
                  {l.label}
                </a>
              </li>
            ))}
          </ul>
          <div className="hidden sm:block">
            <DownloadButton size="sm" hideMessage />
          </div>
        </div>
      </nav>
    </header>
  );
}
