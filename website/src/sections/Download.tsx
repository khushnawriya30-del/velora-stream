import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Download as DownloadIcon, ChevronDown, Clock, ExternalLink, Copy, Check } from 'lucide-react';
import { useGitHubRelease } from '../hooks/useGitHubRelease';
import { APP_CONFIG } from '../config';

export default function Download({ referralCode }: { referralCode?: string | null }) {
  const { latest, releases, loading } = useGitHubRelease();
  const [showHistory, setShowHistory] = useState(false);
  const [copied, setCopied] = useState(false);

  const version = latest?.version || APP_CONFIG.fallback.version;
  const downloadUrl = latest?.downloadUrl || APP_CONFIG.fallback.downloadUrl;

  const handleCopy = () => {
    if (referralCode) {
      navigator.clipboard.writeText(referralCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
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

        {/* Referral Code Banner */}
        {referralCode && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4 }}
            className="mb-8 mx-auto max-w-md"
          >
            <div className="bg-gradient-to-r from-gold/10 via-gold/15 to-gold/10 border border-gold/30 rounded-2xl p-5">
              <p className="text-gold text-sm font-medium mb-2">You were invited! Enter this code when you sign up:</p>
              <div className="flex items-center justify-center gap-3">
                <span className="text-2xl font-bold text-white tracking-[6px] font-mono">{referralCode}</span>
                <button
                  onClick={handleCopy}
                  className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors"
                  title="Copy code"
                >
                  {copied ? (
                    <Check className="w-4 h-4 text-green-400" />
                  ) : (
                    <Copy className="w-4 h-4 text-gray-300" />
                  )}
                </button>
              </div>
              <p className="text-gray-400 text-xs mt-2">Paste this referral code during registration in the app</p>
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
            className="group inline-flex items-center gap-3 px-10 py-4 bg-gradient-to-r from-gold via-gold to-gold-dark text-black font-bold rounded-2xl text-lg hover:opacity-90 transition-all shadow-2xl shadow-gold/25 hover:shadow-gold/40 hover:scale-[1.02] active:scale-[0.98]"
          >
            <DownloadIcon className="w-5 h-5 group-hover:animate-bounce" />
            Download APK v{version}
          </a>
        </motion.div>

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
