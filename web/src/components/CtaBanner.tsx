import Link from "next/link";

export function CtaBanner() {
  return (
    <section className="relative overflow-hidden bg-gradient-to-r from-[#172554] via-[#1D4ED8] to-[#2563EB] py-16 text-white">
      <div
        className="pointer-events-none absolute -right-20 top-0 h-96 w-96 rounded-full bg-blue-400/20 blur-3xl"
        aria-hidden="true"
      />
      <div
        className="pointer-events-none absolute -left-20 bottom-0 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl"
        aria-hidden="true"
      />

      <div className="container-page relative z-10">
        <div className="grid gap-10 lg:grid-cols-2 lg:items-center">
          <div>
            <span className="inline-block rounded-full bg-white/10 px-3 py-1 text-xs font-semibold tracking-wider uppercase text-blue-100 backdrop-blur-sm">
              Instant Setup
            </span>
            <h2 className="mt-4 text-3xl font-semibold tracking-tight sm:text-4xl">
              Ready to eliminate inventory discrepancies?
            </h2>
            <p className="mt-3 text-base text-blue-100/90 max-w-lg leading-relaxed">
              Register your store tenant in 60 seconds, download the desktop application, and onboard your sales staff with one-time join codes.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <Link
                href="/signup"
                className="inline-flex h-12 items-center justify-center rounded-[10px] bg-white px-6 font-semibold text-navy shadow-lg transition-transform hover:-translate-y-0.5 hover:bg-slate-50"
              >
                Register Business Free
              </Link>
              <Link
                href="/download"
                className="inline-flex h-12 items-center justify-center rounded-[10px] border border-white/30 bg-white/10 px-6 font-semibold text-white backdrop-blur-sm transition-colors hover:bg-white/20"
              >
                View Downloads
              </Link>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 sm:gap-6">
            <div className="rounded-xl border border-white/15 bg-white/10 p-6 backdrop-blur-md">
              <p className="text-3xl font-bold tracking-tight sm:text-4xl">99.9%</p>
              <p className="mt-2 text-xs font-medium uppercase tracking-wider text-blue-200">
                Stock Reconciliation Accuracy
              </p>
            </div>
            <div className="rounded-xl border border-white/15 bg-white/10 p-6 backdrop-blur-md">
              <p className="text-3xl font-bold tracking-tight sm:text-4xl">10 Min</p>
              <p className="mt-2 text-xs font-medium uppercase tracking-wider text-blue-200">
                Staff Join Code Expiry
              </p>
            </div>
            <div className="rounded-xl border border-white/15 bg-white/10 p-6 backdrop-blur-md">
              <p className="text-3xl font-bold tracking-tight sm:text-4xl">0</p>
              <p className="mt-2 text-xs font-medium uppercase tracking-wider text-blue-200">
                External Java Dependencies
              </p>
            </div>
            <div className="rounded-xl border border-white/15 bg-white/10 p-6 backdrop-blur-md">
              <p className="text-3xl font-bold tracking-tight sm:text-4xl">100%</p>
              <p className="mt-2 text-xs font-medium uppercase tracking-wider text-blue-200">
                Multi-Tenant Isolation
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
