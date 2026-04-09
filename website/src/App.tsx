import { useState, useEffect } from 'react';
import Navbar from './sections/Navbar';
import Hero from './sections/Hero';
import Features from './sections/Features';
import WhyChooseUs from './sections/WhyChooseUs';
import HowItWorks from './sections/HowItWorks';
import About from './sections/About';
import Pricing from './sections/Pricing';
import TrustSection from './sections/TrustSection';
import FAQ from './sections/FAQ';
import Contact from './sections/Contact';
import Download from './sections/Download';
import Footer from './sections/Footer';
import Terms from './sections/Terms';
import RefundPolicy from './sections/RefundPolicy';
import PrivacyPolicy from './sections/PrivacyPolicy';

export default function App() {
  const [page, setPage] = useState(window.location.pathname);

  useEffect(() => {
    const onPop = () => setPage(window.location.pathname);
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, []);

  // Handle referral link: ?ref=CODE
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const refCode = params.get('ref');
    if (refCode) {
      localStorage.setItem('velora_ref_code', refCode);
      // Clean URL without reload
      const url = new URL(window.location.href);
      url.searchParams.delete('ref');
      window.history.replaceState({}, '', url.pathname);
      // Scroll to download section
      setTimeout(() => {
        document.getElementById('download')?.scrollIntoView({ behavior: 'smooth' });
      }, 500);
    }
  }, []);

  if (page === '/terms') {
    return <Terms />;
  }

  if (page === '/refund-policy') {
    return <RefundPolicy />;
  }

  if (page === '/privacy-policy') {
    return <PrivacyPolicy />;
  }

  return (
    <div className="min-h-screen bg-[#050505] text-white overflow-x-hidden">
      <Navbar />
      <main>
        <Hero />
        <Features />
        <WhyChooseUs />
        <HowItWorks />
        <About />
        <Pricing />
        <TrustSection />
        <Download />
        <FAQ />
        <Contact />
        <Footer />
      </main>
    </div>
  );
}
