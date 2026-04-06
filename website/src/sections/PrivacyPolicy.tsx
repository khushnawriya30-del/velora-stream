import { ArrowLeft } from 'lucide-react';

export default function PrivacyPolicy() {
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
        <h1 className="text-3xl font-bold mb-2">Privacy Policy</h1>
        <p className="text-gray-500 text-sm mb-10">Last updated: April 7, 2026</p>

        <div className="space-y-8 text-gray-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-white mb-3">1. Introduction</h2>
            <p>
              Velora by Vishu Nawariya ("we", "us", "our") operates the Velora platform and application
              ("Service"). This Privacy Policy explains how we collect, use, disclose, and safeguard
              your information when you use our Service. Please read this policy carefully.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">2. Information We Collect</h2>
            <p>We may collect the following types of information:</p>
            <ul className="list-disc list-inside space-y-2 mt-2">
              <li>
                <strong className="text-white">Account Information:</strong> Name, email address, phone number,
                and authentication credentials when you create an account.
              </li>
              <li>
                <strong className="text-white">Payment Information:</strong> UPI transaction IDs and payment
                references for premium subscriptions. We do not store your UPI PIN or bank credentials.
              </li>
              <li>
                <strong className="text-white">Usage Data:</strong> App usage patterns, feature interactions,
                device information, and crash reports to improve our Service.
              </li>
              <li>
                <strong className="text-white">Device Information:</strong> Device model, operating system version,
                unique device identifiers, and network information.
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">3. How We Use Your Information</h2>
            <p>We use the information we collect to:</p>
            <ul className="list-disc list-inside space-y-2 mt-2">
              <li>Provide, maintain, and improve our Service.</li>
              <li>Process premium subscription payments and manage your account.</li>
              <li>Send you important updates, security alerts, and support messages.</li>
              <li>Personalize your experience and provide content recommendations.</li>
              <li>Analyze usage patterns to improve our platform.</li>
              <li>Detect and prevent fraud, abuse, and security threats.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">4. Data Sharing & Disclosure</h2>
            <p>We do <strong className="text-white">not</strong> sell your personal data. We may share information only in these cases:</p>
            <ul className="list-disc list-inside space-y-2 mt-2">
              <li>
                <strong className="text-white">Service Providers:</strong> With trusted third-party services
                (e.g., Firebase, analytics) that help us operate our platform, subject to confidentiality obligations.
              </li>
              <li>
                <strong className="text-white">Legal Requirements:</strong> When required by law, regulation,
                or legal process.
              </li>
              <li>
                <strong className="text-white">Safety:</strong> To protect the rights, property, or safety of
                our users or the public.
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">5. Data Storage & Security</h2>
            <p>
              Your data is stored securely using industry-standard encryption and security measures.
              We use secure cloud infrastructure and follow best practices to protect your information.
              However, no method of electronic transmission or storage is 100% secure, and we cannot
              guarantee absolute security.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">6. Data Retention</h2>
            <p>
              We retain your personal information for as long as your account is active or as needed to
              provide our Service. You may request deletion of your account and associated data at any
              time by contacting us.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">7. Your Rights</h2>
            <p>You have the right to:</p>
            <ul className="list-disc list-inside space-y-2 mt-2">
              <li>Access the personal data we hold about you.</li>
              <li>Request correction of inaccurate personal data.</li>
              <li>Request deletion of your personal data and account.</li>
              <li>Withdraw consent for data processing at any time.</li>
              <li>Request a copy of your data in a portable format.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">8. Children's Privacy</h2>
            <p>
              Our Service is not directed to children under the age of 13. We do not knowingly collect
              personal information from children under 13. If we discover that we have collected data
              from a child under 13, we will delete it promptly.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">9. Third-Party Services</h2>
            <p>
              Our Service may use third-party services such as Firebase (Google), analytics platforms,
              and payment processors. These services have their own privacy policies, and we encourage
              you to review them. We are not responsible for the privacy practices of third-party services.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">10. Changes to This Policy</h2>
            <p>
              We may update this Privacy Policy from time to time. Any changes will be posted on this
              page with an updated date. Continued use of the Service after changes constitutes
              acceptance of the revised policy.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">11. Contact Us</h2>
            <p>
              If you have any questions about this Privacy Policy or wish to exercise your rights,
              please contact us at:{' '}
              <a
                href="mailto:velorastream@gmail.com"
                className="text-gold hover:underline"
              >
                velorastream@gmail.com
              </a>
            </p>
            <p className="mt-2">
              Business: Velora by Vishu Nawariya<br />
              Business Type: Individual / Digital Services<br />
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
