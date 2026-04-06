import { motion } from 'framer-motion';
import { CreditCard, Crown, Rocket, CheckCircle2 } from 'lucide-react';

const steps = [
  {
    number: '01',
    icon: Crown,
    title: 'Choose Your Plan',
    description: 'Browse our flexible subscription plans and select the one that best fits your needs. Plans range from 1 month to 12 months with increasing savings.',
    color: 'from-gold to-gold-dark',
  },
  {
    number: '02',
    icon: CreditCard,
    title: 'Secure Payment',
    description: 'Complete your purchase securely via UPI. All transactions are encrypted and processed through verified payment gateways.',
    color: 'from-blue-500 to-cyan-500',
  },
  {
    number: '03',
    icon: Rocket,
    title: 'Instant Activation',
    description: 'Your premium membership is activated within seconds. Get immediate access to all premium features and exclusive content.',
    color: 'from-emerald-500 to-green-400',
  },
  {
    number: '04',
    icon: CheckCircle2,
    title: 'Enjoy Premium',
    description: 'Explore curated recommendations, enjoy ad-free experiences, HD/4K playback, multi-device sync, and all premium benefits.',
    color: 'from-purple-500 to-pink-500',
  },
];

export default function HowItWorks() {
  return (
    <section className="relative py-28 px-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/3 left-1/4 w-[300px] h-[300px] bg-blue-500/[0.02] rounded-full blur-[100px]" />
        <div className="absolute bottom-1/3 right-1/4 w-[300px] h-[300px] bg-gold/[0.02] rounded-full blur-[100px]" />
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
            <Rocket className="w-4 h-4 text-gold" />
            <span className="text-gold text-sm font-medium">Getting Started</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4">
            How It <span className="gradient-text">Works</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            Get started with Velora in just a few simple steps. From sign-up to premium — it takes less than a minute.
          </p>
        </motion.div>

        {/* Steps */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 relative">
          {/* Connection line (desktop) */}
          <div className="hidden lg:block absolute top-[4.5rem] left-[12%] right-[12%] h-px bg-gradient-to-r from-gold/30 via-blue-500/30 via-emerald-500/30 to-purple-500/30" />

          {steps.map((step, index) => (
            <motion.div
              key={step.number}
              initial={{ opacity: 0, y: 40 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-30px' }}
              transition={{ duration: 0.5, delay: index * 0.12 }}
              className="relative text-center"
            >
              {/* Step number ring */}
              <div className="relative mx-auto w-[7rem] h-[7rem] mb-6">
                <div className={`absolute inset-0 rounded-full bg-gradient-to-br ${step.color} opacity-10`} />
                <div className="absolute inset-[3px] rounded-full bg-[#0A0A0A] flex items-center justify-center">
                  <div className="text-center">
                    <step.icon className="w-7 h-7 text-gold mx-auto mb-1" />
                    <span className="text-xs text-gray-500 font-mono">{step.number}</span>
                  </div>
                </div>
                {/* Glow */}
                <div className={`absolute -inset-2 rounded-full bg-gradient-to-br ${step.color} opacity-5 blur-xl`} />
              </div>

              <h3 className="text-lg font-semibold text-white mb-2">{step.title}</h3>
              <p className="text-gray-400 text-sm leading-relaxed max-w-[280px] mx-auto">
                {step.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
