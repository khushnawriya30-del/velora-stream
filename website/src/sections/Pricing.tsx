import { motion } from 'framer-motion';
import { Crown, Check } from 'lucide-react';

const plans = [
  {
    id: '1m',
    name: '1 Month',
    price: 159,
    originalPrice: 182,
    discount: 10,
    months: 1,
    badge: null,
    featured: false,
    features: [
      'Premium Member Access',
      'Ad-Free Experience',
      'HD Quality Playback',
      'Multi-Device Support',
    ],
  },
  {
    id: '3m',
    name: '3 Months',
    price: 459,
    originalPrice: 543,
    discount: 15,
    months: 3,
    badge: 'Most Popular',
    featured: true,
    features: [
      'Premium Member Access',
      'Ad-Free Experience',
      'HD & 4K Quality Playback',
      'Multi-Device Support',
      'Priority Support',
    ],
  },
  {
    id: '6m',
    name: '6 Months',
    price: 829,
    originalPrice: 1038,
    discount: 20,
    months: 6,
    badge: null,
    featured: false,
    features: [
      'Premium Member Access',
      'Ad-Free Experience',
      'HD & 4K Quality Playback',
      'Multi-Device Support',
      'Priority Support',
    ],
  },
  {
    id: '12m',
    name: '12 Months',
    price: 1529,
    originalPrice: 2042,
    discount: 25,
    months: 12,
    badge: 'Best Value',
    featured: false,
    features: [
      'Premium Member Access',
      'Ad-Free Experience',
      'HD & 4K Quality Playback',
      'Multi-Device Support',
      'Priority Support',
      'Exclusive Features',
    ],
  },
];

export default function Pricing() {
  return (
    <section id="pricing" className="relative py-28 px-6 overflow-hidden">
      {/* Background ambient glow */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[700px] bg-gold/[0.04] rounded-full blur-[150px]" />
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
            <Crown className="w-4 h-4 text-gold" />
            <span className="text-gold text-sm font-medium">Membership Plans</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4">
            Choose Your <span className="gradient-text">Plan</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            Unlock premium membership features. All prices in ₹ INR.
          </p>
        </motion.div>

        {/* Plans grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {plans.map((plan, index) => {
            const perMonth = Math.round(plan.price / plan.months);
            return (
              <motion.div
                key={plan.id}
                initial={{ opacity: 0, y: 40 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: '-50px' }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
                whileHover={{ y: -8, transition: { duration: 0.3 } }}
                className={`relative group rounded-2xl p-[1px] ${
                  plan.featured
                    ? 'bg-gradient-to-b from-gold via-gold/50 to-gold/20 shadow-2xl shadow-gold/10'
                    : 'bg-gradient-to-b from-white/[0.12] to-white/[0.04]'
                }`}
              >
                {/* Badge */}
                {plan.badge && (
                  <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 z-10">
                    <div
                      className={`px-4 py-1 rounded-full text-xs font-bold whitespace-nowrap ${
                        plan.featured
                          ? 'bg-gradient-to-r from-gold to-gold-dark text-black shadow-lg shadow-gold/30'
                          : 'bg-gold/15 text-gold border border-gold/30'
                      }`}
                    >
                      {plan.badge}
                    </div>
                  </div>
                )}

                <div
                  className={`rounded-2xl p-6 h-full flex flex-col ${
                    plan.featured
                      ? 'bg-[#0A0A0A]'
                      : 'bg-[#0A0A0A]/80 hover:bg-[#0F0F0F]'
                  } transition-colors duration-300`}
                >
                  {/* Plan name */}
                  <h3
                    className={`text-lg font-semibold mb-5 ${
                      plan.featured ? 'text-gold' : 'text-white'
                    }`}
                  >
                    {plan.name}
                  </h3>

                  {/* Price block */}
                  <div className="mb-1">
                    <div className="flex items-baseline gap-1">
                      <span className="text-base text-gray-400 font-medium">₹</span>
                      <span
                        className={`text-4xl font-extrabold tracking-tight ${
                          plan.featured ? 'text-gold' : 'text-white'
                        }`}
                      >
                        {plan.price.toLocaleString('en-IN')}
                      </span>
                    </div>
                    {plan.months > 1 && (
                      <p className="text-sm text-gray-500 mt-1">
                        ₹{perMonth}/month
                      </p>
                    )}
                  </div>

                  {/* Original price & discount */}
                  <div className="flex items-center gap-2 mb-6">
                    <span className="text-sm text-gray-500 line-through">
                      ₹{plan.originalPrice.toLocaleString('en-IN')}
                    </span>
                    <span className="text-xs font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full">
                      {plan.discount}% OFF
                    </span>
                  </div>

                  {/* Divider */}
                  <div
                    className={`h-px mb-6 ${
                      plan.featured
                        ? 'bg-gradient-to-r from-transparent via-gold/30 to-transparent'
                        : 'bg-white/[0.06]'
                    }`}
                  />

                  {/* Features */}
                  <ul className="space-y-3 mb-8 flex-1">
                    {plan.features.map((feature) => (
                      <li key={feature} className="flex items-center gap-2.5">
                        <div
                          className={`w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0 ${
                            plan.featured
                              ? 'bg-gold/20 text-gold'
                              : 'bg-white/[0.06] text-gray-400'
                          }`}
                        >
                          <Check className="w-3 h-3" strokeWidth={3} />
                        </div>
                        <span className="text-sm text-gray-300">{feature}</span>
                      </li>
                    ))}
                  </ul>

                  {/* CTA */}
                  <a
                    href="#download"
                    className={`block w-full py-3 rounded-xl text-center text-sm font-semibold transition-all duration-300 ${
                      plan.featured
                        ? 'bg-gradient-to-r from-gold to-gold-dark text-black hover:shadow-lg hover:shadow-gold/25 hover:opacity-90'
                        : 'bg-white/[0.06] text-white hover:bg-white/[0.1] border border-white/[0.08]'
                    }`}
                  >
                    Get Started
                  </a>
                </div>
              </motion.div>
            );
          })}
        </div>

        {/* Bottom note */}
        <motion.p
          className="text-center text-gray-500 text-sm mt-12"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.5 }}
        >
          All plans include a 7-day money-back guarantee · Pay securely via UPI · See our <a href="/refund-policy" className="text-gold hover:underline">Refund Policy</a>
        </motion.p>
      </div>
    </section>
  );
}
