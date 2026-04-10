import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Download as DownloadIcon, ChevronDown, Clock, ExternalLink, Smartphone, CheckCircle } from 'lucide-react';
import { useGitHubRelease } from '../hooks/useGitHubRelease';
import { APP_CONFIG } from '../config';

export default function Download({ referralCode }: { referralCode?: string | null }) {
  const { latest, releases, loading } = useGitHubRelease();
  const [showHistory, setShowHistory] = useState(false);
  const [hasDownloaded, setHasDownloaded] = useState(false);

  const version = latest?.version || APP_CONFIG.fallback.version;
  const downloadUrl = latest?.downloadUrl || APP_CONFIG.fallback.downloadUrl;

  // Build Android intent deep link (more reliable than custom scheme on Android)
  const deepLinkUrl = referralCode
    ? `intent://referral?code=${referralCode}#Intent;scheme=velora;package=com.cinevault.app;end`
    : null;

  // Fallback custom scheme link
  const customSchemeUrl = referralCode ? `velora://referral?code=${referralCode}` : null;

  const tryOpenApp = useCallback(() => {
    if (!referralCode) return;
    
    const isAndroid = /android/i.test(navigator.userAgent);
    
    if (isAndroid && deepLinkUrl) {
      // Try intent:// first, then fallback to velora:// after a delay
      window.location.href = deepLinkUrl;
      // If intent:// fails (some browsers don't support it), try custom scheme after 1.5s
      setTimeout(() => {
        if (customSchemeUrl) {
          window.location.href = customSchemeUrl;
        }
      }, 1500);
    } else if (customSchemeUrl) {
      window.location.href = customSchemeUrl;
    }
  }, [referralCode, deepLinkUrl, customSchemeUrl]);

  // After download, auto-try to open app when user returns to the page
  useEffect(() => {
    if (!hasDownloaded || !referralCode) return;

    // Try once after a short delay (in case app was already installed)
    const timer = setTimeout(() => tryOpenApp(), 3000);

    // Also try when page becomes visible again (user installed app & came back to browser)
    const handleVisibility = () => {
      if (document.visibilityState === 'visible') {
        setTimeout(() => tryOpenApp(), 500);
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);

    return () => {
      clearTimeout(timer);
      document.removeEventListener('visibilitychange', handleVisibility);
    };
  }, [hasDownloaded, referralCode, tryOpenApp]);

  const handleDownloadClick = () => {
    if (referralCode) {
      setHasDownloaded(true);
    }
  };

  return (
    <section id="download" className="relative py-28 px-6">
      {/* Background gradient */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-gold/5 rounded-full blur-[150px]" />
      </div>

      <div className="relative z-10 max-w-2xl mx-auto text-center">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="text-3xl sm:text-4xl font-bold mb-4">
            Get <span className="gradient-text">{APP_CONFIG.name}</span>
          </h2>
          <p className="text-gray-400 mb-10 text-lg">
            Download the latest version and get started.
          </p>
        </motion.div>

        {/* Referral Active Banner */}
        {referralCode && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4 }}
            className="mb-8 mx-auto max-w-md"
          >
            <div className="bg-gradient-to-r from-green-500/10 via-emerald-500/15 to-green-500/10 border border-green-500/30 rounded-2xl p-4">
              <div className="flex items-center justify-center gap-2 text-green-400">
                <CheckCircle className="w-5 h-5" />
                <p className="text-sm font-medium">Referral link detected! Your referral will be applied automatically.</p>
              </div>
            </div>
          </motion.div>
        )}

        {/* Download Button */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.1 }}
        >
          <a
            href={downloadUrl || '#'}
            target="_blank"
            rel="noopener noreferrer"
            onClick={handleDownloadClick}
            className="group inline-flex items-center gap-3 px-10 py-4 bg-gradient-to-r from-gold via-gold to-gold-dark text-black font-bold rounded-2xl text-lg hover:opacity-90 transition-all shadow-2xl shadow-gold/25 hover:shadow-gold/40 hover:scale-[1.02] active:scale-[0.98]"
          >
            <DownloadIcon className="w-5 h-5 group-hover:animate-bounce" />
            Download APK v{version}
          </a>
        </motion.div>

        {/* Post-download: Open App button (auto-redirect also happening in background) */}
        <AnimatePresence>
          {referralCode && hasDownloaded && (
            <motion.div
              initial={{ opacity: 0, y: 20, height: 0 }}
              animate={{ opacity: 1, y: 0, height: 'auto' }}
              exit={{ opacity: 0, y: -10, height: 0 }}
              transition={{ duration: 0.5 }}
              className="mt-6"
            >
              <div className="bg-white/5 border border-white/10 rounded-2xl p-5 max-w-md mx-auto">
                <p className="text-gray-400 text-sm mb-3">After installing, tap to open the app:</p>
                <a
                  href={customSchemeUrl || '#'}
                  onClick={(e) => { e.preventDefault(); tryOpenApp(); }}
                  className="group inline-flex items-center gap-3 px-8 py-3 bg-gradient-to-r from-green-500 to-emerald-600 text-white font-bold rounded-xl text-base hover:opacity-90 transition-all shadow-lg hover:scale-[1.02] active:scale-[0.98]"
                >
                  <Smartphone className="w-5 h-5" />
                  Open Velora
                </a>
                <p className="text-gray-500 text-xs mt-3">Referral will be applied automatically when you sign up</p>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Already installed shortcut */}
        {referralCode && !hasDownloaded && (
          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            transition={{ delay: 0.2 }}
            className="mt-4"
          >
            <a
              href={customSchemeUrl || '#'}
              onClick={(e) => { e.preventDefault(); tryOpenApp(); }}
              className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-green-400 transition-colors"
            >
              <Smartphone className="w-4 h-4" />
              Already installed? Open app
            </a>
          </motion.div>
        )}

        {/* Version History Toggle */}
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ delay: 0.3 }}
          className="mt-8"
        >
          <button
            onClick={() => setShowHistory(!showHistory)}
            className="inline-flex items-center gap-2 px-6 py-3 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 transition-all text-sm text-gray-300"
          >
            <Clock className="w-4 h-4 text-gold" />
            Version History
            <ChevronDown
              className={`w-4 h-4 transition-transform duration-300 ${
                showHistory ? 'rotate-180' : ''
              }`}
            />
          </button>
        </motion.div>

        {/* Version History List */}
        <AnimatePresence>
          {showHistory && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.4, ease: 'easeInOut' }}
              className="overflow-hidden"
            >
              <div className="mt-6 max-h-[400px] overflow-y-auto space-y-3 text-left">
                {releases.length === 0 && !loading && (
                  <p className="text-center text-gray-500 py-4 text-sm">
                    No releases found. Configure your GitHub repository in config.ts
                  </p>
                )}

                {releases.map((release, i) => (
                  <motion.div
                    key={release.tagName}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.05 }}
                    className={`p-4 rounded-xl border transition-colors ${
                      i === 0
                        ? 'bg-gold/5 border-gold/20'
                        : 'bg-white/[0.02] border-white/5 hover:border-white/10'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span
                          className={`font-semibold ${
                            i === 0 ? 'text-gold' : 'text-white'
                          }`}
                        >
                          v{release.version}
                        </span>
                        {i === 0 && (
                          <span className="px-2 py-0.5 rounded-full bg-gold/20 text-gold text-xs font-medium">
                            Latest
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-gray-500">
                        {new Date(release.publishedAt).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                        })}
                      </span>
                    </div>

                    {release.body && (
                      <p className="text-sm text-gray-400 whitespace-pre-line line-clamp-3 mb-2">
                        {release.body}
                      </p>
                    )}

                    {release.downloadUrl && (
                      <a
                        href={release.downloadUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 text-xs text-gold hover:text-gold-light transition-colors"
                      >
                        Download APK <ExternalLink className="w-3 h-3" />
                      </a>
                    )}
                  </motion.div>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </section>
  );
}
