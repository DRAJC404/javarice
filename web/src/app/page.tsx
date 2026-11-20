import { About } from "@/components/About";
import { Footer } from "@/components/Footer";
import { Hero } from "@/components/Hero";
import { Install } from "@/components/Install";
import { Nav } from "@/components/Nav";

export default function Home() {
  return (
    <>
      <a href="#main" className="skip-link">
        Skip to content
      </a>
      <Nav />
      <main id="main">
        <Hero />
        <About />
        <Install />
      </main>
      <Footer />
    </>
  );
}
