import { useState, useEffect } from 'react';
import Navbar from './sections/Navbar';
import Hero from './sections/Hero';
import Features from './sections/Features';
import About from './sections/About';
import Pricing from './sections/Pricing';
import FAQ from './sections/FAQ';
import Contact from './sections/Contact';
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
        <About />
        <Pricing />
        <FAQ />
        <Contact />
        <Footer />
      </main>
    </div>
  );
}
