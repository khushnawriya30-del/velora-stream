import { motion } from 'framer-motion';
import {
  Crown,
  Ban,
  MonitorPlay,
  Sparkles,
  Smartphone,
  Shield,
  RefreshCw,
  Zap,
  Bell,
  Palette,
  Headphones,
  Download,
} from 'lucide-react';
import { APP_CONFIG } from '../config';

const iconMap: Record<string, React.ElementType> = {
  Crown,
  Ban,
  MonitorPlay,
  Sparkles,
  Smartphone,
  Shield,
  RefreshCw,
  Zap,
  Bell,
  Palette,
  Headphones,
  Download,
};

export default function Features() {
  return (
    <section id="features" className="relative py-28 px-6">
      {/* Background */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-gold/[0.02] rounded-full blur-[150px]" />
      </div>

      <div className="max-w-6xl mx-auto relative">
        {/* Section header */}
        <motion.div
          className="text-center mb-6"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
        >
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-gold/10 border border-gold/20 mb-6">
            <Sparkles className="w-4 h-4 text-gold" />
            <span className="text-gold text-sm font-medium">Platform Features</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4">
            Everything You Need,{' '}
            <span className="gradient-text">Nothing You Don't</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            A comprehensive suite of premium features designed for the ultimate digital membership experience.
          </p>
        </motion.div>

        {/* Feature grid - 12 items in 3x4 or 4x3 */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 mt-14">
          {APP_CONFIG.features.map((feature, index) => {
            const Icon = iconMap[feature.icon] || Crown;
            return (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: '-30px' }}
                transition={{ duration: 0.4, delay: index * 0.05 }}
                whileHover={{ y: -4, transition: { duration: 0.2 } }}
                className="group p-6 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-gold/20 hover:bg-white/[0.04] transition-all duration-300"
              >
                <div
                  className={`w-12 h-12 rounded-xl bg-gradient-to-br ${feature.gradient} flex items-center justify-center mb-4 shadow-lg group-hover:scale-110 transition-transform duration-300`}
                >
                  <Icon className="w-5 h-5 text-white" />
                </div>

                <h3 className="text-base font-semibold mb-2 text-white">
                  {feature.title}
                </h3>
                <p className="text-gray-400 text-sm leading-relaxed">
                  {feature.description}
                </p>
              </motion.div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
