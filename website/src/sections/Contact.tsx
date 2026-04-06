import { motion } from 'framer-motion';
import { Mail, MapPin, Clock, MessageCircle, Send, ExternalLink } from 'lucide-react';
import { APP_CONFIG } from '../config';

const contactMethods = [
  {
    icon: Mail,
    title: 'Email Support',
    description: 'Send us an email anytime',
    value: APP_CONFIG.business.email,
    href: `mailto:${APP_CONFIG.business.email}`,
    gradient: 'from-gold to-gold-dark',
    iconColor: 'text-black',
    isLink: true,
  },
  {
    icon: Clock,
    title: 'Response Time',
    description: 'We value your time',
    value: 'Within 24 hours',
    href: null,
    gradient: 'from-blue-500 to-cyan-500',
    iconColor: 'text-white',
    isLink: false,
  },
  {
    icon: MapPin,
    title: 'Location',
    description: 'Based in',
    value: APP_CONFIG.business.location,
    href: null,
    gradient: 'from-emerald-500 to-green-400',
    iconColor: 'text-white',
    isLink: false,
  },
  {
    icon: MessageCircle,
    title: 'Business Type',
    description: 'Operating as',
    value: APP_CONFIG.business.type,
    href: null,
    gradient: 'from-purple-500 to-pink-500',
    iconColor: 'text-white',
    isLink: false,
  },
];

export default function Contact() {
  return (
    <section id="contact" className="relative py-28 px-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute bottom-1/4 right-1/4 w-[400px] h-[400px] bg-gold/[0.03] rounded-full blur-[120px]" />
      </div>

      <div className="max-w-5xl mx-auto relative">
        {/* Section header */}
        <motion.div
          className="text-center mb-16"
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
        >
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-gold/10 border border-gold/20 mb-6">
            <Send className="w-4 h-4 text-gold" />
            <span className="text-gold text-sm font-medium">Contact Us</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4">
            Get in <span className="gradient-text">Touch</span>
          </h2>
          <p className="text-gray-400 text-lg max-w-xl mx-auto">
            Have questions, feedback, or need support? We're here to help and would love to hear from you.
          </p>
        </motion.div>

        {/* Contact cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-12">
          {contactMethods.map((method, index) => {
            const Wrapper = method.isLink ? motion.a : motion.div;
            const wrapperProps = method.isLink ? { href: method.href ?? undefined } : {};

            return (
              <Wrapper
                key={method.title}
                {...wrapperProps}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: index * 0.08 }}
                whileHover={{ y: -4 }}
                className="group p-8 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-gold/20 transition-all duration-300"
              >
                <div className="flex items-start gap-5">
                  <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${method.gradient} flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform flex-shrink-0`}>
                    <method.icon className={`w-6 h-6 ${method.iconColor}`} />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold mb-1 text-white flex items-center gap-2">
                      {method.title}
                      {method.isLink && <ExternalLink className="w-3.5 h-3.5 text-gray-500" />}
                    </h3>
                    <p className="text-gray-500 text-sm mb-2">{method.description}</p>
                    <p className={`text-sm font-medium ${method.isLink ? 'text-gold' : 'text-gray-300'}`}>
                      {method.value}
                    </p>
                  </div>
                </div>
              </Wrapper>
            );
          })}
        </div>

        {/* CTA banner */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, delay: 0.3 }}
          className="p-8 rounded-2xl bg-gradient-to-r from-gold/10 to-gold/5 border border-gold/20 text-center"
        >
          <h3 className="text-xl font-semibold text-white mb-3">Ready to Get Started?</h3>
          <p className="text-gray-400 text-sm mb-6 max-w-lg mx-auto">
            Join thousands of premium members and experience the best digital subscription platform.
          </p>
          <a
            href="#pricing"
            className="inline-flex items-center gap-2 px-8 py-3 bg-gradient-to-r from-gold to-gold-dark text-black font-bold rounded-full text-sm hover:opacity-90 transition-all shadow-lg shadow-gold/25"
          >
            View Plans
            <ExternalLink className="w-4 h-4" />
          </a>
        </motion.div>
      </div>
    </section>
  );
}
