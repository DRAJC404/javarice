import Image from "next/image";
import { basePath } from "@/config/site";

const rows = [
  {
    number: "01",
    title: "Real-time stock deductions",
    body: "Sold, used, damaged and expired goods are tracked automatically, so the count on screen always matches the shelf.",
    image: "stock.png",
    alt: "bAI stock analytics screen showing total deductions and a daily stock movement chart",
  },
  {
    number: "02",
    title: "Predictive restock alerts",
    body: "Velocity monitoring and demand forecasting warn you before an item runs out, not after.",
    image: "alerts.png",
    alt: "bAI insights screen listing items that need restocking next to a seven-day demand forecast",
  },
  {
    number: "03",
    title: "High-precision inventory audits",
    body: "Reconcile a shift in minutes and run spot checks across every location.",
    image: "audits.png",
    alt: "bAI inventory control screen listing each product's stock level against its par threshold",
  },
];

export function About() {
  return (
    <section id="about" aria-labelledby="about-title">
      <div className="container-page py-16 wide:py-24">
        <h2 id="about-title" className="sr-only">
          About bAI
        </h2>
        <div className="space-y-16 wide:space-y-24">
          {rows.map((row, i) => {
            const flipped = i % 2 === 1;
            return (
              <article
                key={row.number}
                className="about-row group grid items-center gap-8 wide:grid-cols-2 wide:gap-16"
              >
                <div className={flipped ? "wide:order-2" : undefined}>
                  <p className="about-number text-[16px] font-semibold text-primary">{row.number}</p>
                  <h3 className="about-title mt-2 text-[28px] font-semibold leading-[1.15] tracking-[-0.02em] text-navy sm:text-[36px]">
                    {row.title}
                  </h3>
                  <p className="mt-4 max-w-[460px] text-[17px] leading-[1.6] text-muted">{row.body}</p>
                </div>
                <div className={flipped ? "wide:order-1" : undefined}>
                  <div className="about-shot relative h-[300px] overflow-hidden rounded-[12px] border border-line bg-tint">
                    <Image
                      src={`${basePath}/screens/${row.image}`}
                      alt={row.alt}
                      fill
                      sizes="(min-width: 900px) 528px, 100vw"
                      className="object-cover object-left-top"
                    />
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}
