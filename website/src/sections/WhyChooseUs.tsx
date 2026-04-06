import { motion } from 'framer-motion';
import { Zap, Gem, IndianRupee, Lock, Rocket } from 'lucide-react';

const reasons = [
  {
    icon: Zap,
    title: 'Blazing Fast Performance',
    description: 'Built on optimized infrastructure with global CDN for lightning-fast content delivery and zero buffering.',
    stat: '< 2s',
    statLabel: 'Load Time',
    gradient: 'from-yellow-500 to-orange-400',
  },
  {
    icon: Gem,
    title: 'Premium Experience',
    description: 'Beautifully crafted interface with intuitive navigation, dark theme, and attention to every detail.',
    stat: '4.8★',
    statLabel: 'User Rating',
    gradient: 'from-purple-500 to-pink-500',
  },
  {
    icon: IndianRupee,
    title: 'Affordable Pricing',
    description: 'Premium features at prices that work for everyone. Plans starting from just ₹159/month with discounts up to 25%.',
    stat: '₹159',
    statLabel: 'Starting Price',
    gradient: 'from-emerald-500 to-green-400',
  },
  {
    icon: Lock,
    title: 'Secure Payments',
    description: 'All payments processed through encrypted, verified UPI gateways. Your financial data is never stored on our servers.',
    stat: '256-bit',
    statLabel: 'Encryption',
    gradient: 'from-blue-500 to-cyan-500',
  },
  {
    icon: Rocket,
    title: 'Instant Activation',
    description: 'No waiting. Your premium membership is activated instantly after successful payment confirmation.',
    stat: '< 5s',
    statLabel: 'Activation',
    gradient: 'from-gold to-gold-dark',
  },
];

export default function WhyChooseUs() {
  return (
    <section className="relative py-28 px-6 overflow-hidden">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-gold/20 to-transparent" />
        <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-gold/20 to-transparent" />
        <div className="absolute top-1/2 right-0 w-[400px] h-[400px] bg-gold/[0.02] rounded-full blur-[120px]" />
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
            <span className="text-gold text-sm font-medium">Why Velora</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4">
            Why Choose <span className="gradient-text">Velora</span>?
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            We're committed to delivering the best digital membership experience with unmatched quality and value.
          </p>
        </motion.div>

        {/* Reasons grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {reasons.map((reason, index) => (
            <motion.div
              key={reason.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-30px' }}
              transition={{ duration: 0.5, delay: index * 0.08 }}
              whileHover={{ y: -4, transition: { duration: 0.2 } }}
              className={`group relative p-7 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-gold/20 transition-all duration-300 ${
                index === 4 ? 'md:col-span-2 lg:col-span-1' : ''
              }`}
            >
              {/* Icon */}
              <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${reason.gradient} flex items-center justify-center mb-5 shadow-lg group-hover:scale-110 transition-transform`}>
                <reason.icon className="w-5 h-5 text-white" />
              </div>

              {/* Content */}
              <h3 className="text-lg font-semibold mb-2 text-white">{reason.title}</h3>
              <p className="text-gray-400 text-sm leading-relaxed mb-5">{reason.description}</p>

              {/* Stat */}
              <div className="pt-4 border-t border-white/5">
                <p className="text-2xl font-bold text-gold">{reason.stat}</p>
                <p className="text-xs text-gray-500 mt-0.5">{reason.statLabel}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
