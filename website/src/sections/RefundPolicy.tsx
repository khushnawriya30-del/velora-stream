import { ArrowLeft } from 'lucide-react';

export default function RefundPolicy() {
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
        <h1 className="text-3xl font-bold mb-2">Refund Policy</h1>
        <p className="text-gray-500 text-sm mb-10">Last updated: April 6, 2026</p>

        <div className="space-y-8 text-gray-300 leading-relaxed">
          <section>
            <h2 className="text-xl font-semibold text-white mb-3">1. Overview</h2>
            <p>
              This Refund Policy outlines the terms under which refunds may be issued for premium
              subscriptions purchased through the VELORA application. Please read this policy carefully
              before making a purchase.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">2. No Refund After Code Generation</h2>
            <p>
              Once a premium activation code has been generated and delivered to you, the transaction is
              considered complete and <strong className="text-white">no refund will be issued</strong>. This is because
              activation codes are single-use digital goods that cannot be revoked once generated.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">3. Eligible Refund Scenarios</h2>
            <p>Refunds may be considered in the following cases:</p>
            <ul className="list-disc list-inside space-y-2 mt-2">
              <li>
                <strong className="text-white">Duplicate Payment:</strong> If you were charged multiple times for the
                same plan due to a technical error.
              </li>
              <li>
                <strong className="text-white">Payment Made but No Code Received:</strong> If your payment was
                successfully processed but you did not receive an activation code within 24 hours.
              </li>
              <li>
                <strong className="text-white">Code Not Working:</strong> If the activation code provided does not work
                and our support team is unable to resolve the issue.
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">4. How to Request a Refund</h2>
            <p>To request a refund, please follow these steps:</p>
            <ol className="list-decimal list-inside space-y-2 mt-2">
              <li>
                Email us at{' '}
                <a href="mailto:velorastream@gmail.com" className="text-gold hover:underline">
                  velorastream@gmail.com
                </a>{' '}
                with the subject line "Refund Request".
              </li>
              <li>Include your UTR / Transaction ID from the UPI payment.</li>
              <li>Include your Telegram username used during purchase.</li>
              <li>Describe the reason for the refund request.</li>
              <li>Attach a screenshot of the payment confirmation if available.</li>
            </ol>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">5. Refund Processing Time</h2>
            <p>
              If your refund request is approved, the refund will be processed within{' '}
              <strong className="text-white">5-7 business days</strong> to the original UPI payment method. Please
              note that actual credit to your account may take additional time depending on your bank.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">6. Non-Refundable Situations</h2>
            <p>Refunds will <strong className="text-white">not</strong> be issued in the following cases:</p>
            <ul className="list-disc list-inside space-y-2 mt-2">
              <li>You changed your mind after the activation code was generated.</li>
              <li>You purchased the wrong plan (please contact support for plan changes).</li>
              <li>Your account was suspended due to violation of our Terms & Conditions.</li>
              <li>The refund request is made more than 7 days after the purchase.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">7. Chargebacks</h2>
            <p>
              If you initiate a chargeback or payment dispute without first contacting us, your VELORA
              account may be permanently suspended. We encourage you to reach out to our support team
              first to resolve any issues.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">8. Changes to This Policy</h2>
            <p>
              We reserve the right to modify this Refund Policy at any time. Any changes will be effective
              immediately upon posting on this page. Continued use of the App constitutes acceptance of
              the updated policy.
            </p>
          </section>

          <section>
            <h2 className="text-xl font-semibold text-white mb-3">9. Contact Us</h2>
            <p>
              For any refund-related queries, please contact us at:{' '}
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
