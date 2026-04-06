import { motion } from 'framer-motion';
import { Layers, Search, Gem, Target, Heart, Globe } from 'lucide-react';
import { APP_CONFIG } from '../config';

const highlights = [
  {
    icon: Layers,
    title: 'Digital Subscription Services',
    description:
      'Velora offers flexible subscription plans that unlock premium features, enhanced user experience, and exclusive member benefits for digital media consumption.',
  },
  {
    icon: Search,
    title: 'Curated Content Discovery',
    description:
      'Our intelligent recommendation engine uses advanced algorithms to help you discover new digital experiences tailored to your unique preferences.',
  },
  {
    icon: Gem,
    title: 'Premium Experience',
    description:
      'Enjoy an ad-free, high-quality interface with multi-device sync, personalized settings, 4K playback, and priority customer support.',
  },
  {
    icon: Target,
    title: 'Our Mission',
    description:
      'To democratize access to premium digital experiences by providing world-class features at affordable prices for users across India.',
  },
  {
    icon: Heart,
    title: 'User-Centric Design',
    description:
      'Every feature is designed with users in mind. We continuously iterate based on feedback to deliver the best possible experience.',
  },
  {
    icon: Globe,
    title: 'Made in India',
    description:
      'Proudly built and operated from India, optimized for Indian users with local payment support and regional content preferences.',
  },
];

export default function About() {
  return (
    <section id="about" className="relative py-28 px-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/2 left-1/4 w-[400px] h-[400px] bg-gold/[0.03] rounded-full blur-[120px]" />
        <div className="absolute bottom-1/4 right-1/4 w-[300px] h-[300px] bg-purple-500/[0.02] rounded-full blur-[100px]" />
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
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-gold/10 border border-gold/20 mb-6">
            <Gem className="w-4 h-4 text-gold" />
            <span className="text-gold text-sm font-medium">About Us</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4">
            About <span className="gradient-text">{APP_CONFIG.name}</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-3xl mx-auto">
            {APP_CONFIG.brandFull} is a premium digital subscription and content discovery platform
            built to deliver exceptional features, intelligent recommendations, and an unmatched user experience
            for modern digital consumers.
          </p>
        </motion.div>

        {/* Highlights grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {highlights.map((item, index) => (
            <motion.div
              key={item.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-30px' }}
              transition={{ duration: 0.5, delay: index * 0.08 }}
              whileHover={{ y: -4, transition: { duration: 0.2 } }}
              className="group text-center p-7 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-gold/20 transition-all duration-300"
            >
              <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br from-gold to-gold-dark flex items-center justify-center mb-5 shadow-lg group-hover:scale-110 transition-transform">
                <item.icon className="w-6 h-6 text-black" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-white">{item.title}</h3>
              <p className="text-gray-400 text-sm leading-relaxed">{item.description}</p>
            </motion.div>
          ))}
        </div>

        {/* Business info */}
        <motion.div
          className="mt-16"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          <div className="p-8 rounded-2xl bg-white/[0.02] border border-white/5">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
              <div>
                <p className="text-xs text-gray-500 uppercase tracking-wider mb-2">Brand</p>
                <p className="text-sm text-white font-semibold">{APP_CONFIG.business.name}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500 uppercase tracking-wider mb-2">Business Type</p>
                <p className="text-sm text-white font-semibold">{APP_CONFIG.business.type}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500 uppercase tracking-wider mb-2">Location</p>
                <p className="text-sm text-white font-semibold">{APP_CONFIG.business.location}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500 uppercase tracking-wider mb-2">Contact</p>
                <p className="text-sm text-gold font-semibold">{APP_CONFIG.business.email}</p>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
