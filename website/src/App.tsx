import { useState, useEffect } from 'react';
import Navbar from './sections/Navbar';
import Hero from './sections/Hero';
import Download from './sections/Download';
import Features from './sections/Features';
import AppPreview from './sections/AppPreview';
import FAQ from './sections/FAQ';
import Footer from './sections/Footer';
import Terms from './sections/Terms';

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

  return (
    <div className="min-h-screen bg-[#050505] text-white overflow-x-hidden">
      <Navbar />
      <main>
        <Hero />
        <Download />
        <Features />
        <AppPreview />
        <FAQ />
        <Footer />
      </main>
    </div>
  );
}
