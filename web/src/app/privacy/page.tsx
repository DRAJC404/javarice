import { Nav } from "@/components/Nav";
import { Footer } from "@/components/Footer";
import { site } from "@/config/site";

export default function PrivacyPage() {
  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col justify-between">
      <Nav />
      <main className="container-page py-12 md:py-20 max-w-3xl flex-1">
        <h1 className="text-3xl font-semibold tracking-tight text-navy sm:text-4xl">
          Privacy Policy
        </h1>
        <p className="mt-2 text-sm text-muted">Last updated: November 2026</p>

        <div className="prose prose-sm mt-8 space-y-6 text-muted leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-navy">1. Overview</h2>
            <p className="mt-2">
              At {site.name}, we believe your inventory, sales, and retail operational data belong solely to you. We do not sell, rent, or monetize your store data or visitor records.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-navy">2. Telemetry and Download Logging</h2>
            <p className="mt-2">
              When you download {site.name} native binaries from our portal, we record anonymized download events (OS platform, architecture, packaging kind, and application version). To protect your privacy:
            </p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>IP addresses are irreversibly hashed using salted SHA-256 before storage.</li>
              <li>We never store raw IP addresses or identifiable browser fingerprints.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-navy">3. Lead Capture & Direct Links</h2>
            <p className="mt-2">
              If you submit your email address to receive download links on mobile, your email is used exclusively to dispatch the requested links and optional product release updates if you explicitly consented.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-navy">4. Desktop Application Security</h2>
            <p className="mt-2">
              The {site.name} Desktop application runs locally on your workstation. Inventory counts, barcoding records, and staff accounts are scoped to your organization tenant boundary.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-navy">5. Contact Us</h2>
            <p className="mt-2">
              If you have any questions or data privacy requests, contact us at{" "}
              <a href={`mailto:${site.supportEmail}`} className="text-primary underline">
                {site.supportEmail}
              </a>.
            </p>
          </section>
        </div>
      </main>
      <Footer />
    </div>
  );
}
