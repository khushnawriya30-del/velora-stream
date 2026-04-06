import { motion } from 'framer-motion';
import { Layers, Search, Gem } from 'lucide-react';
import { APP_CONFIG } from '../config';

const highlights = [
  {
    icon: Layers,
    title: 'Digital Subscription Services',
    description:
      'Velora offers flexible subscription plans that unlock premium features, enhanced user experience, and exclusive member benefits.',
  },
  {
    icon: Search,
    title: 'Curated Content Discovery',
    description:
      'Our intelligent recommendation engine helps you discover new digital experiences tailored to your preferences and interests.',
  },
  {
    icon: Gem,
    title: 'Premium Experience',
    description:
      'Enjoy an ad-free, high-quality interface with multi-device sync, personalized settings, and priority customer support.',
  },
];

export default function About() {
  return (
    <section id="about" className="relative py-28 px-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/2 left-1/4 w-[400px] h-[400px] bg-gold/[0.03] rounded-full blur-[120px]" />
      </div>

      <div className="max-w-6xl mx-auto relative">
        {/* Section header */}
        <motion.div
          className="text-center mb-16"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="text-3xl sm:text-4xl font-bold mb-4">
            About <span className="gradient-text">{APP_CONFIG.name}</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            {APP_CONFIG.brandFull} is a digital subscription and content discovery platform built
            to deliver premium features, curated recommendations, and an exceptional user experience.
          </p>
        </motion.div>

        {/* Highlights grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {highlights.map((item, index) => (
            <motion.div
              key={item.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-50px' }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="text-center p-8 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-gold/20 transition-colors duration-300"
            >
              <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br from-gold to-gold-dark flex items-center justify-center mb-5 shadow-lg">
                <item.icon className="w-6 h-6 text-black" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-white">{item.title}</h3>
              <p className="text-gray-400 text-sm leading-relaxed">{item.description}</p>
            </motion.div>
          ))}
        </div>

        {/* Business info */}
        <motion.div
          className="mt-16 text-center"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          <div className="inline-flex flex-col sm:flex-row items-center gap-6 sm:gap-10 px-8 py-5 rounded-2xl bg-white/[0.02] border border-white/5">
            <div className="text-center">
              <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Brand</p>
              <p className="text-sm text-white font-medium">{APP_CONFIG.business.name}</p>
            </div>
            <div className="hidden sm:block w-px h-8 bg-white/10" />
            <div className="text-center">
              <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Business Type</p>
              <p className="text-sm text-white font-medium">{APP_CONFIG.business.type}</p>
            </div>
            <div className="hidden sm:block w-px h-8 bg-white/10" />
            <div className="text-center">
              <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Location</p>
              <p className="text-sm text-white font-medium">{APP_CONFIG.business.location}</p>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
