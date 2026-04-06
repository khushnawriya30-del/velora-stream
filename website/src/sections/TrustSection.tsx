import { motion } from 'framer-motion';
import { Shield, Clock, Award, Zap, Lock, RefreshCw } from 'lucide-react';
import { APP_CONFIG } from '../config';

const trustItems = [
  {
    icon: Shield,
    title: 'Secure Payments',
    description: 'All transactions are encrypted with 256-bit SSL security through verified payment gateways.',
  },
  {
    icon: Clock,
    title: '24/7 Support',
    description: 'Our dedicated support team is available round the clock to assist you via email.',
  },
  {
    icon: Award,
    title: 'Trusted Platform',
    description: 'Trusted by 10,000+ premium members across India with a 4.8-star average rating.',
  },
  {
    icon: Zap,
    title: 'Instant Activation',
    description: 'Your premium features are activated within seconds of successful payment.',
  },
  {
    icon: Lock,
    title: 'Data Privacy',
    description: 'We follow strict data protection practices. Your information is never sold or shared.',
  },
  {
    icon: RefreshCw,
    title: '7-Day Money Back',
    description: 'Not satisfied? Get a full refund within 7 days of purchase. No questions asked.',
  },
];

export default function TrustSection() {
  return (
    <section className="relative py-28 px-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-emerald-500/[0.02] rounded-full blur-[120px]" />
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
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 mb-6">
            <Shield className="w-4 h-4 text-emerald-400" />
            <span className="text-emerald-400 text-sm font-medium">Trust & Security</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4">
            Your Trust Is Our <span className="gradient-text">Priority</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            We're committed to providing a safe, secure, and transparent platform for all our members.
          </p>
        </motion.div>

        {/* Trust grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {trustItems.map((item, index) => (
            <motion.div
              key={item.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-30px' }}
              transition={{ duration: 0.4, delay: index * 0.08 }}
              className="group p-7 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-emerald-500/20 transition-all duration-300"
            >
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-500 to-green-400 flex items-center justify-center mb-4 shadow-lg group-hover:scale-110 transition-transform">
                <item.icon className="w-5 h-5 text-white" />
              </div>
              <h3 className="text-lg font-semibold mb-2 text-white">{item.title}</h3>
              <p className="text-gray-400 text-sm leading-relaxed">{item.description}</p>
            </motion.div>
          ))}
        </div>

        {/* Bottom trust bar */}
        <motion.div
          className="mt-14 p-6 rounded-2xl bg-white/[0.02] border border-white/5"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          <div className="flex flex-col md:flex-row items-center justify-center gap-8 md:gap-16 text-center">
            <div>
              <p className="text-3xl font-bold text-gold">10,000+</p>
              <p className="text-xs text-gray-500 mt-1">Active Members</p>
            </div>
            <div className="hidden md:block w-px h-10 bg-white/10" />
            <div>
              <p className="text-3xl font-bold text-gold">4.8★</p>
              <p className="text-xs text-gray-500 mt-1">Average Rating</p>
            </div>
            <div className="hidden md:block w-px h-10 bg-white/10" />
            <div>
              <p className="text-3xl font-bold text-gold">99.9%</p>
              <p className="text-xs text-gray-500 mt-1">Uptime</p>
            </div>
            <div className="hidden md:block w-px h-10 bg-white/10" />
            <div>
              <p className="text-3xl font-bold text-gold">24hrs</p>
              <p className="text-xs text-gray-500 mt-1">Support Response</p>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
