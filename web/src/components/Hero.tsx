import Image from "next/image";
import { basePath } from "@/config/site";
import { DownloadButton } from "./DownloadButton";

export function Hero() {
  return (
    <section
      id="top"
      aria-labelledby="hero-title"
      className="relative overflow-x-clip bg-gradient-to-b from-[#EEF4FF] via-[#F8FAFF] to-[#F4F7FD] border-b border-line/60"
    >
      <div
        className="pointer-events-none absolute -left-40 top-0 h-[480px] w-[480px] rounded-full bg-primary/10 blur-[100px]"
        aria-hidden="true"
      />
      <div
        className="pointer-events-none absolute right-0 top-1/4 h-[420px] w-[420px] rounded-full bg-blue-300/20 blur-[120px]"
        aria-hidden="true"
      />

      <div className="container-page relative z-10 grid items-center gap-12 py-16 wide:grid-cols-[5fr_7fr] wide:gap-12 wide:py-24">
        <div>
          <h1
            id="hero-title"
            className="fade-up text-[40px] font-semibold leading-[1.05] tracking-[-0.03em] text-navy sm:text-[52px] wide:text-[60px]"
          >
            Smarter stock tracking, zero manual counting
          </h1>
          <p className="fade-up delay-1 mt-6 max-w-[440px] text-[18px] leading-[1.6] text-muted">
            bAI keeps your inventory accurate as you sell, so you always know what&rsquo;s on the shelf.
          </p>
          <div className="fade-up delay-2 mt-8 flex flex-wrap items-center gap-x-6 gap-y-4">
            <DownloadButton />
            <a href="#install" className="text-link text-[16px] font-medium">
              Other platforms
            </a>
          </div>
          <p className="fade-up delay-3 mt-4 text-[14px] text-muted">Available for Windows, macOS and Linux</p>
        </div>

        <div className="hero-window">
          <div className="window">
            <div className="flex h-9 items-center gap-2 border-b border-line bg-tint px-4" aria-hidden="true">
              <span className="h-[10px] w-[10px] rounded-full bg-[#CBD3E3]" />
              <span className="h-[10px] w-[10px] rounded-full bg-[#CBD3E3]" />
              <span className="h-[10px] w-[10px] rounded-full bg-[#CBD3E3]" />
            </div>
            <Image
              src={`${basePath}/screens/hero.png`}
              alt="bAI dashboard showing stock totals, low-stock alerts and a seven-day stock movement chart"
              width={2400}
              height={1500}
              priority
              sizes="(min-width: 900px) 760px, 100vw"
              className="block h-auto w-full"
            />
          </div>
        </div>
      </div>
    </section>
  );
}
