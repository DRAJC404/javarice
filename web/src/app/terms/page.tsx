import { Nav } from "@/components/Nav";
import { Footer } from "@/components/Footer";
import { site } from "@/config/site";

export default function TermsPage() {
  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col justify-between">
      <Nav />
      <main className="container-page py-12 md:py-20 max-w-3xl flex-1">
        <h1 className="text-3xl font-semibold tracking-tight text-navy sm:text-4xl">
          Terms of Service
        </h1>
        <p className="mt-2 text-sm text-muted">Last updated: November 2026</p>

        <div className="prose prose-sm mt-8 space-y-6 text-muted leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-navy">1. Acceptance of Terms</h2>
            <p className="mt-2">
              By accessing {site.name} web distribution services or installing {site.name} Desktop software, you agree to comply with and be bound by these Terms of Service.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-navy">2. Software License</h2>
            <p className="mt-2">
              {site.name} grants you a non-exclusive, revocable license to install and execute the desktop software on store terminals, workstations, and personal computers within your business organization.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-navy">3. Account & Workstation Security</h2>
            <p className="mt-2">
              Business owners are responsible for maintaining the confidentiality of their credentials and staff join codes. Join codes expire after 10 minutes and must not be shared outside authorized team members.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-navy">4. Disclaimer of Warranties</h2>
            <p className="mt-2">
              The software and services are provided &ldquo;as is&rdquo; without warranties of any kind. While we strive for 100% stock tracking accuracy, businesses should perform routine spot audits and reconciliations.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-navy">5. Contact</h2>
            <p className="mt-2">
              Questions regarding these terms may be sent to{" "}
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
