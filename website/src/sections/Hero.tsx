import { motion } from 'framer-motion';
import { Crown, Shield, Zap, Users } from 'lucide-react';
import { APP_CONFIG } from '../config';

const stats = [
  { icon: Users, value: '10K+', label: 'Active Members' },
  { icon: Crown, value: '4.8★', label: 'User Rating' },
  { icon: Shield, value: '100%', label: 'Secure Payments' },
  { icon: Zap, value: '99.9%', label: 'Uptime' },
];

export default function Hero() {
  return (
    <section
      id="hero"
      className="relative min-h-screen flex items-center justify-center overflow-hidden pt-16"
    >
      {/* Background effects */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[800px] h-[800px] bg-gold/[0.04] rounded-full blur-[150px] animate-glow-pulse" />
        <div className="absolute top-1/3 left-1/4 w-[300px] h-[300px] bg-purple-500/[0.03] rounded-full blur-[100px]" />
        <div className="absolute top-1/2 right-1/4 w-[250px] h-[250px] bg-blue-500/[0.03] rounded-full blur-[100px]" />
        <div className="absolute bottom-0 left-0 right-0 h-40 bg-gradient-to-t from-[#050505] to-transparent" />
        {/* Grid pattern */}
        <div className="absolute inset-0 opacity-[0.03]" style={{
          backgroundImage: 'linear-gradient(rgba(212,175,55,0.3) 1px, transparent 1px), linear-gradient(90deg, rgba(212,175,55,0.3) 1px, transparent 1px)',
          backgroundSize: '60px 60px',
        }} />
      </div>

      <div className="relative z-10 max-w-6xl mx-auto px-6 py-24 text-center">
        {/* Badge */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1, duration: 0.6 }}
          className="inline-flex items-center gap-2 px-5 py-2 rounded-full bg-gold/10 border border-gold/20 mb-10"
        >
          <div className="w-2 h-2 rounded-full bg-gold animate-pulse" />
          <span className="text-gold text-sm font-medium">Trusted by 10,000+ Members Across India</span>
        </motion.div>

        {/* Heading */}
        <motion.h1
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2, duration: 0.8, ease: 'easeOut' }}
          className="text-5xl sm:text-6xl lg:text-7xl xl:text-8xl font-extrabold tracking-tight leading-[1.05] mb-6"
        >
          <span className="gradient-text">{APP_CONFIG.name}</span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35, duration: 0.6 }}
          className="text-2xl sm:text-3xl lg:text-4xl font-semibold text-white/90 mb-6"
        >
          {APP_CONFIG.tagline}
        </motion.p>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.6 }}
          className="text-gray-400 text-lg sm:text-xl max-w-2xl mx-auto mb-12 leading-relaxed"
        >
          {APP_CONFIG.description}
        </motion.p>

        {/* CTAs */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.65, duration: 0.6 }}
          className="flex flex-col sm:flex-row items-center gap-4 justify-center mb-20"
        >
          <a
            href="#pricing"
            className="group px-10 py-4 bg-gradient-to-r from-gold to-gold-dark text-black font-bold rounded-full text-base hover:opacity-90 transition-all shadow-xl shadow-gold/25 hover:shadow-gold/40 flex items-center gap-2"
          >
            <Crown className="w-5 h-5" />
            Get Premium Access
          </a>
          <a
            href="#features"
            className="px-10 py-4 border border-white/10 text-white font-medium rounded-full text-base hover:bg-white/5 hover:border-white/20 transition-all"
          >
            Explore Features
          </a>
        </motion.div>

        {/* Stats */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.8, duration: 0.6 }}
          className="grid grid-cols-2 md:grid-cols-4 gap-6 max-w-3xl mx-auto"
        >
          {stats.map((stat, index) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.9 + index * 0.1, duration: 0.5 }}
              className="p-5 rounded-2xl bg-white/[0.03] border border-white/[0.06] hover:border-gold/20 transition-colors"
            >
              <stat.icon className="w-5 h-5 text-gold mx-auto mb-2" />
              <p className="text-2xl font-bold text-white">{stat.value}</p>
              <p className="text-xs text-gray-500 mt-1">{stat.label}</p>
            </motion.div>
          ))}
        </motion.div>
      </div>

      {/* Scroll indicator */}
      <motion.div
        className="absolute bottom-8 left-1/2 -translate-x-1/2"
        animate={{ y: [0, 8, 0] }}
        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
      >
        <div className="w-6 h-10 rounded-full border-2 border-white/20 flex items-start justify-center pt-2">
          <div className="w-1.5 h-3 bg-gold rounded-full" />
        </div>
      </motion.div>
    </section>
  );
}
