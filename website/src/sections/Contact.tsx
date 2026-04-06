import { motion } from 'framer-motion';
import { Mail, MapPin, Clock, MessageCircle } from 'lucide-react';
import { APP_CONFIG } from '../config';

export default function Contact() {
  return (
    <section id="contact" className="relative py-28 px-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute bottom-1/4 right-1/4 w-[400px] h-[400px] bg-gold/[0.03] rounded-full blur-[120px]" />
      </div>

      <div className="max-w-4xl mx-auto relative">
        {/* Section header */}
        <motion.div
          className="text-center mb-16"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="text-3xl sm:text-4xl font-bold mb-4">
            Get in <span className="gradient-text">Touch</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-xl mx-auto">
            Have questions or need support? We're here to help.
          </p>
        </motion.div>

        {/* Contact cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <motion.a
            href={`mailto:${APP_CONFIG.business.email}`}
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5 }}
            whileHover={{ y: -4 }}
            className="group p-8 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-gold/20 transition-all duration-300 text-center"
          >
            <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br from-gold to-gold-dark flex items-center justify-center mb-5 shadow-lg group-hover:scale-110 transition-transform">
              <Mail className="w-6 h-6 text-black" />
            </div>
            <h3 className="text-lg font-semibold mb-2 text-white">Email Support</h3>
            <p className="text-gold text-sm">{APP_CONFIG.business.email}</p>
          </motion.a>

          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="p-8 rounded-2xl bg-white/[0.02] border border-white/5 text-center"
          >
            <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br from-blue-500 to-cyan-500 flex items-center justify-center mb-5 shadow-lg">
              <Clock className="w-6 h-6 text-white" />
            </div>
            <h3 className="text-lg font-semibold mb-2 text-white">Response Time</h3>
            <p className="text-gray-400 text-sm">We typically respond within 24 hours</p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="p-8 rounded-2xl bg-white/[0.02] border border-white/5 text-center"
          >
            <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br from-emerald-500 to-green-400 flex items-center justify-center mb-5 shadow-lg">
              <MapPin className="w-6 h-6 text-white" />
            </div>
            <h3 className="text-lg font-semibold mb-2 text-white">Location</h3>
            <p className="text-gray-400 text-sm">{APP_CONFIG.business.location}</p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, delay: 0.3 }}
            className="p-8 rounded-2xl bg-white/[0.02] border border-white/5 text-center"
          >
            <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center mb-5 shadow-lg">
              <MessageCircle className="w-6 h-6 text-white" />
            </div>
            <h3 className="text-lg font-semibold mb-2 text-white">Business Type</h3>
            <p className="text-gray-400 text-sm">{APP_CONFIG.business.type}</p>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
