import { ArrowLeft } from 'lucide-react';

export default function Terms() {
  return (
    <div className="min-h-screen bg-[#050505] text-white">
      {/* Header */}
      <div className="sticky top-0 z-50 bg-[#050505]/80 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-6 py-4 flex items-center gap-4">
          <a
            href="/"
            className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            Back
          </a>
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-gold to-gold-dark flex items-center justify-center">
            <span className="text-black font-extrabold text-sm">V</span>
          </div>
          <span className="text-white font-semibold">VELORA</span>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-4xl mx-auto px-6 py-12">
        <h1 className="text-3xl font-bold mb-2">Terms of Service</h1>
        <p className="text-gray-500 text-sm mb-10">Last updated: April 7, 2026</p>

        <div className="space-y-8 text-gray-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-white mb-3">1. Acceptance of Terms</h2>
            <p>
              By accessing or using the Velora platform and application ("Service"), operated by
              Velora by Vishu Nawariya ("we", "us", "our"), you agree to be bound
              by these Terms of Service. If you do not agree to these terms, please do not use the Service.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">2. Description of Service</h2>
            <p>
              Velora is a digital subscription and content discovery platform that provides premium
              membership features, curated content recommendations, personalized user experiences,
              and enhanced platform capabilities. The Service may include both free and premium features.
              Premium features require a valid subscription.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">3. User Accounts</h2>
            <ul className="list-disc list-inside space-y-2">
              <li>You must provide accurate and complete information when creating an account.</li>
              <li>You are responsible for maintaining the confidentiality of your account credentials.</li>
              <li>You must not share your account with others or allow unauthorized access.</li>
              <li>We reserve the right to suspend or terminate accounts that violate these terms.</li>
              <li>You must be at least 13 years of age to create an account.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">4. Premium Subscription</h2>
            <ul className="list-disc list-inside space-y-2">
              <li>Premium features are available through paid subscription plans (1, 3, 6, or 12 months).</li>
              <li>All prices are listed in Indian Rupees (₹ INR).</li>
              <li>Payments are processed securely via UPI.</li>
              <li>Subscriptions are non-refundable once the activation code is generated, except as outlined in our Refund Policy.</li>
              <li>Activation codes are single-use and tied to one account.</li>
              <li>We reserve the right to modify pricing and plans at any time with prior notice.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">5. Acceptable Use</h2>
            <p>You agree not to:</p>
            <ul className="list-disc list-inside space-y-2 mt-2">
              <li>Use the Service for any unlawful purpose.</li>
              <li>Attempt to reverse engineer, decompile, or modify the application.</li>
              <li>Interfere with or disrupt the Service's infrastructure or servers.</li>
              <li>Use automated tools or bots to access the Service.</li>
              <li>Violate the intellectual property rights of others.</li>
              <li>Attempt to gain unauthorized access to other users' accounts.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">6. Intellectual Property</h2>
            <p>
              The Velora name, logo, and application design are the intellectual property of
              Velora by Vishu Nawariya. All content, features, and functionality of the Service
              are owned by us and are protected by applicable intellectual property laws.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">7. Privacy</h2>
            <p>
              Your use of the Service is also governed by our Privacy Policy. We collect minimal data
              necessary to provide our services. We do not sell your personal data to third parties.
              Please review our <a href="/privacy-policy" className="text-gold hover:underline">Privacy Policy</a> for
              complete details.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">8. Disclaimer of Warranties</h2>
            <p>
              The Service is provided "as is" and "as available" without warranties of any kind,
              either express or implied. We do not guarantee uninterrupted or error-free service.
              We are not responsible for any loss or damage arising from the use of the Service.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">9. Limitation of Liability</h2>
            <p>
              To the maximum extent permitted by applicable law, Velora by Vishu Nawariya shall not be
              liable for any indirect, incidental, special, consequential, or punitive damages arising
              from the use or inability to use the Service.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">10. Governing Law</h2>
            <p>
              These Terms shall be governed by and construed in accordance with the laws of India.
              Any disputes arising under these Terms shall be subject to the exclusive jurisdiction
              of the courts in India.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">11. Changes to Terms</h2>
            <p>
              We reserve the right to update these Terms of Service at any time. Changes will be
              effective immediately upon posting. Continued use of the Service after changes
              constitutes acceptance of the updated terms.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">12. Contact Us</h2>
            <p>
              If you have any questions about these Terms of Service, please contact us at:{' '}
              <a
                href="mailto:velorastream@gmail.com"
                className="text-gold hover:underline"
              >
                velorastream@gmail.com
              </a>
            </p>
            <p className="mt-2">
              Business: Velora by Vishu Nawariya<br />
              Location: India
            </p>
          </section>
        </div>

        {/* Footer */}
        <div className="mt-16 pt-8 border-t border-white/5 text-center text-sm text-gray-500">
          &copy; {new Date().getFullYear()} Velora by Vishu Nawariya. All rights reserved.
        </div>
      </div>
    </div>
  );
}
