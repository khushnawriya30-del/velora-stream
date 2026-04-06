import { motion } from 'framer-motion';
import { Mail, MapPin, Shield, Crown } from 'lucide-react';
import { APP_CONFIG } from '../config';

export default function Footer() {
  return (
    <footer className="relative py-16 px-6 border-t border-white/5">
      {/* Background glow */}
      <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-[400px] h-[200px] bg-gold/5 rounded-full blur-[100px] pointer-events-none" />

      <div className="relative z-10 max-w-6xl mx-auto">
        {/* Top section: 4 columns */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-10 mb-12">
          {/* Brand */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
            className="sm:col-span-2 md:col-span-1"
          >
            <div className="flex items-center gap-2.5 mb-4">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-gold to-gold-dark flex items-center justify-center shadow-lg shadow-gold/20">
                <span className="text-black font-extrabold text-lg">V</span>
              </div>
              <span className="font-bold text-lg">{APP_CONFIG.name}</span>
            </div>
            <p className="text-gray-400 text-sm leading-relaxed mb-4">
              {APP_CONFIG.brandFull} — A premium digital subscription platform providing
              curated experiences and exclusive member benefits.
            </p>
            <div className="flex items-center gap-2 text-gray-500 text-sm">
              <MapPin className="w-4 h-4" />
              <span>{APP_CONFIG.business.location}</span>
            </div>
          </motion.div>

          {/* Quick Links */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, delay: 0.1 }}
          >
            <h4 className="text-white font-semibold mb-4">Quick Links</h4>
            <div className="flex flex-col gap-3">
              <a href="#features" className="text-sm text-gray-400 hover:text-white transition-colors">Features</a>
              <a href="#about" className="text-sm text-gray-400 hover:text-white transition-colors">About</a>
              <a href="#pricing" className="text-sm text-gray-400 hover:text-white transition-colors">Pricing</a>
              <a href="#faq" className="text-sm text-gray-400 hover:text-white transition-colors">FAQ</a>
              <a href="#contact" className="text-sm text-gray-400 hover:text-white transition-colors">Contact</a>
            </div>
          </motion.div>

          {/* Legal */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            <h4 className="text-white font-semibold mb-4">Legal</h4>
            <div className="flex flex-col gap-3">
              <a href="/privacy-policy" className="text-sm text-gray-400 hover:text-white transition-colors flex items-center gap-1.5">
                <Shield className="w-3.5 h-3.5" /> Privacy Policy
              </a>
              <a href="/terms" className="text-sm text-gray-400 hover:text-white transition-colors">Terms of Service</a>
              <a href="/refund-policy" className="text-sm text-gray-400 hover:text-white transition-colors">Refund Policy</a>
            </div>
          </motion.div>

          {/* Support */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            <h4 className="text-white font-semibold mb-4">Support</h4>
            <div className="flex flex-col gap-3">
              <a
                href={`mailto:${APP_CONFIG.business.email}`}
                className="text-sm text-gray-400 hover:text-white transition-colors flex items-center gap-1.5"
              >
                <Mail className="w-3.5 h-3.5" /> {APP_CONFIG.business.email}
              </a>
              <a href="#contact" className="text-sm text-gray-400 hover:text-white transition-colors">Contact Us</a>
              <a href="#faq" className="text-sm text-gray-400 hover:text-white transition-colors">Help & FAQ</a>
            </div>
          </motion.div>
        </div>

        {/* Trust badges */}
        <div className="flex flex-wrap items-center justify-center gap-6 mb-10">
          <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-white/[0.03] border border-white/5 text-xs text-gray-400">
            <Shield className="w-4 h-4 text-emerald-400" />
            256-bit SSL Secured
          </div>
          <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-white/[0.03] border border-white/5 text-xs text-gray-400">
            <Crown className="w-4 h-4 text-gold" />
            10,000+ Members
          </div>
          <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-white/[0.03] border border-white/5 text-xs text-gray-400">
            <Mail className="w-4 h-4 text-blue-400" />
            24hr Support
          </div>
        </div>

        {/* Divider */}
        <div className="w-full h-px bg-gradient-to-r from-transparent via-white/10 to-transparent mb-8" />

        {/* Bottom bar */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-xs text-gray-500">
            &copy; {new Date().getFullYear()} {APP_CONFIG.brandFull}. All rights reserved.
          </p>
          <p className="text-xs text-gray-600">
            {APP_CONFIG.business.type} · {APP_CONFIG.business.location}
          </p>
        </div>
      </div>
    </footer>
  );
}
