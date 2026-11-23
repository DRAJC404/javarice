"use client";

import { useEffect, useRef, useState, type KeyboardEvent, type ReactNode } from "react";
import { downloadUrl, site, type FileKey } from "@/config/site";
import { detectOS, osLabel } from "@/lib/detectOS";

type Tab = keyof typeof osLabel;

const tabs: Tab[] = ["windows", "macos", "linux"];
const { files } = site;

function FileLink({ file, children }: { file: FileKey; children?: ReactNode }) {
  return (
    <a href={downloadUrl(file)} className="text-blue-400 hover:text-blue-300 font-medium underline underline-offset-4">
      {children ?? files[file]}
    </a>
  );
}

function Strong({ children }: { children: ReactNode }) {
  return <strong className="font-semibold text-white">{children}</strong>;
}

const openStep = (
  <>
    Open bAI. Sign in, or choose <Strong>Join with a code</Strong> if your owner invited you.
  </>
);

interface CodeSnippet {
  label: string;
  code: string;
}

const content: Record<Tab, { steps: ReactNode[]; snippets: CodeSnippet[] }> = {
  windows: {
    steps: [
      <>
        Download <FileLink file="windowsMsi" />.
      </>,
      <>
        Run the installer. If SmartScreen warns you, choose <Strong>More info</Strong>, then{" "}
        <Strong>Run anyway</Strong>.
      </>,
      openStep,
    ],
    snippets: [
      {
        label: "PowerShell",
        code: `cd $env:USERPROFILE\\Downloads\nmsiexec /i ${files.windowsMsi}`,
      },
    ],
  },
  macos: {
    steps: [
      <>
        Download <FileLink file="macAppleSilicon">bAI for Apple Silicon</FileLink>.{" "}
        <FileLink file="macIntel">Intel Mac?</FileLink>
      </>,
      <>Open the .dmg and drag bAI to your Applications folder.</>,
      <>
        On first launch, right-click bAI in Applications and choose <Strong>Open</Strong>. Then sign
        in, or choose <Strong>Join with a code</Strong> if your owner invited you.
      </>,
    ],
    snippets: [
      {
        label: "Terminal",
        code: `hdiutil attach ~/Downloads/${files.macAppleSilicon}\ncp -R /Volumes/bAI/bAI.app /Applications/`,
      },
    ],
  },
  linux: {
    steps: [
      <>
        Download the package for your distribution: <FileLink file="linuxDeb" />,{" "}
        <FileLink file="linuxRpm" /> or <FileLink file="linuxAppImage" />.
      </>,
      <>Install it from the folder you downloaded it to with the matching command.</>,
      openStep,
    ],
    snippets: [
      { label: "Debian / Ubuntu", code: `sudo apt install ./${files.linuxDeb}` },
      { label: "Fedora", code: `sudo dnf install ./${files.linuxRpm}` },
      {
        label: "AppImage",
        code: `chmod +x ${files.linuxAppImage} && ./${files.linuxAppImage}`,
      },
    ],
  },
};

function CopyButton({ text, label }: { text: string; label: string }) {
  const [copied, setCopied] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => () => clearTimeout(timer.current), []);

  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      clearTimeout(timer.current);
      timer.current = setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  }

  return (
    <button type="button" onClick={copy} className="copy-btn" aria-label={`Copy ${label} command`}>
      <span aria-live="polite">{copied ? "Copied" : "Copy"}</span>
    </button>
  );
}

export function Install() {
  const [active, setActive] = useState<Tab>("windows");
  const tabRefs = useRef<Partial<Record<Tab, HTMLButtonElement | null>>>({});

  useEffect(() => {
    const os = detectOS();
    if (os !== "unknown") setActive(os);
  }, []);

  function onKeyDown(e: KeyboardEvent<HTMLDivElement>) {
    const i = tabs.indexOf(active);
    const next: Record<string, Tab | undefined> = {
      ArrowRight: tabs[(i + 1) % tabs.length],
      ArrowLeft: tabs[(i - 1 + tabs.length) % tabs.length],
      Home: tabs[0],
      End: tabs[tabs.length - 1],
    };
    const target = next[e.key];
    if (!target) return;
    e.preventDefault();
    setActive(target);
    tabRefs.current[target]?.focus();
  }

  return (
    <section id="install" aria-labelledby="install-title" className="border-t border-slate-800 bg-[#0B1324] text-white">
      <div className="container-page py-16 wide:py-24">
        <h2
          id="install-title"
          className="text-[32px] font-semibold leading-[1.15] tracking-[-0.02em] text-white sm:text-[40px]"
        >
          How to install
        </h2>
        <p className="mt-4 max-w-[560px] text-[17px] leading-[1.6] text-slate-400">
          Choose your operating system and follow the steps.
        </p>

        <div
          role="tablist"
          aria-label="Operating system"
          onKeyDown={onKeyDown}
          className="mt-8 inline-flex rounded-[12px] border border-slate-700 bg-slate-900/90 p-1"
        >
          {tabs.map((tab) => {
            const selected = tab === active;
            return (
              <button
                key={tab}
                ref={(el) => {
                  tabRefs.current[tab] = el;
                }}
                type="button"
                role="tab"
                id={`tab-${tab}`}
                aria-selected={selected}
                aria-controls={`panel-${tab}`}
                tabIndex={selected ? 0 : -1}
                onClick={() => setActive(tab)}
                className={`rounded-[8px] px-4 py-2 text-[14px] font-medium transition-all ${
                  selected
                    ? "bg-primary text-white shadow"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                {osLabel[tab]}
              </button>
            );
          })}
        </div>

        {tabs.map((tab) => (
          <div
            key={tab}
            role="tabpanel"
            id={`panel-${tab}`}
            aria-labelledby={`tab-${tab}`}
            hidden={tab !== active}
            className="mt-8 grid items-start gap-8 wide:grid-cols-2 wide:gap-16"
          >
            <ol className="space-y-6">
              {content[tab].steps.map((step, i) => (
                <li key={i} className="install-step grid grid-cols-[32px_1fr] gap-4">
                  <span
                    aria-hidden="true"
                    className="step-number flex h-8 w-8 items-center justify-center rounded-full border border-slate-700 bg-slate-800 text-[14px] font-semibold text-blue-400"
                  >
                    {i + 1}
                  </span>
                  <p className="pt-1 text-[17px] leading-[1.6] text-slate-300">
                    <span className="sr-only">Step {i + 1}: </span>
                    {step}
                  </p>
                </li>
              ))}
            </ol>

            <div className="code-block rounded-[12px] border border-slate-800 bg-[#050A14] p-2 shadow-2xl">
              {content[tab].snippets.map((s, i) => (
                <div key={s.label} className={i > 0 ? "border-t border-slate-800" : undefined}>
                  <div className="flex items-center justify-between gap-4 px-4 pt-3">
                    <span className="text-[13px] font-medium text-slate-400">{s.label}</span>
                    <CopyButton text={s.code} label={s.label} />
                  </div>
                  <pre className="overflow-x-auto px-4 pb-4 pt-2 font-mono text-[14px] leading-[1.7] text-blue-100">
                    <code>{s.code}</code>
                  </pre>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
