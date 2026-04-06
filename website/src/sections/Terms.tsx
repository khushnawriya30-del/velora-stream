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
        <h1 className="text-3xl font-bold mb-2">Terms & Conditions</h1>
        <p className="text-gray-500 text-sm mb-10">Last updated: April 6, 2026</p>

        <div className="space-y-8 text-gray-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-white mb-3">1. Acceptance of Terms</h2>
            <p>
              By downloading, installing, or using the VELORA application ("App"), you agree to be bound
              by these Terms & Conditions. If you do not agree to these terms, please do not use the App.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">2. Description of Service</h2>
            <p>
              VELORA is a streaming application that provides access to movies, series, and other media
              content. The App may include both free and premium features. Premium features require a
              valid subscription or activation code.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">3. User Accounts</h2>
            <ul className="list-disc list-inside space-y-2">
              <li>You must provide accurate information when creating an account.</li>
              <li>You are responsible for maintaining the confidentiality of your account credentials.</li>
              <li>You must not share your account with others or allow unauthorized access.</li>
              <li>We reserve the right to suspend or terminate accounts that violate these terms.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">4. Premium Subscription</h2>
            <ul className="list-disc list-inside space-y-2">
              <li>Premium features are available through paid subscription plans.</li>
              <li>Payments are processed via UPI and are non-refundable once the activation code is generated.</li>
              <li>Activation codes are single-use and tied to one account.</li>
              <li>We reserve the right to modify pricing and plans at any time.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">5. Acceptable Use</h2>
            <p>You agree not to:</p>
            <ul className="list-disc list-inside space-y-2 mt-2">
              <li>Use the App for any unlawful purpose.</li>
              <li>Attempt to reverse engineer, decompile, or modify the App.</li>
              <li>Distribute, share, or resell content accessed through the App.</li>
              <li>Interfere with or disrupt the App's services or servers.</li>
              <li>Use automated tools to access the App or its content.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">6. Content</h2>
            <p>
              All content provided through the App is for personal, non-commercial use only. We do not
              claim ownership of third-party content. Content availability may vary and can be added or
              removed at any time without notice.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">7. Privacy</h2>
            <p>
              We collect minimal data necessary to provide our services, including account information
              and usage analytics. We do not sell your personal data to third parties. By using the App,
              you consent to our data collection practices.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">8. Disclaimer of Warranties</h2>
            <p>
              The App is provided "as is" without warranties of any kind, either express or implied. We
              do not guarantee uninterrupted or error-free service. We are not responsible for any loss
              or damage arising from the use of the App.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">9. Limitation of Liability</h2>
            <p>
              To the maximum extent permitted by law, VELORA and its developers shall not be liable for
              any indirect, incidental, special, or consequential damages arising from the use or inability
              to use the App.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">10. Changes to Terms</h2>
            <p>
              We reserve the right to update these Terms & Conditions at any time. Continued use of the
              App after changes constitutes acceptance of the updated terms.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">11. Contact Us</h2>
            <p>
              If you have any questions about these Terms & Conditions, please contact us at:{' '}
              <a
                href="mailto:velorastream@gmail.com"
                className="text-gold hover:underline"
              >
                velorastream@gmail.com
              </a>
            </p>
          </section>
        </div>

        {/* Footer */}
        <div className="mt-16 pt-8 border-t border-white/5 text-center text-sm text-gray-500">
          &copy; {new Date().getFullYear()} VELORA. All rights reserved.
        </div>
      </div>
    </div>
  );
}
