import { Controller, Get, Param, Res, Logger } from '@nestjs/common';
import { Response } from 'express';
import { UpiPaymentService } from './upi-payment.service';

@Controller('pay')
export class PaymentPageController {
  private readonly logger = new Logger(PaymentPageController.name);

  constructor(private readonly upiPaymentService: UpiPaymentService) {}

  @Get(':paymentId')
  async renderPaymentPage(
    @Param('paymentId') paymentId: string,
    @Res() res: Response,
  ) {
    const session = await this.upiPaymentService.getPaymentSession(paymentId);
    if (!session) {
      return res.status(404).send(this.renderErrorPage('Payment session not found'));
    }

    if (session.status === 'expired') {
      return res.send(this.renderExpiredPage(session));
    }

    if (session.status === 'verified') {
      return res.send(this.renderSuccessPage(session));
    }

    this.logger.log(`Payment page rendered: ${paymentId}`);
    return res.send(this.renderPaymentHTML(session));
  }

  private renderPaymentHTML(session: any): string {
    const qrData = encodeURIComponent(session.upiLink);
    const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=${qrData}`;
    const amount = (session.uniqueAmount || session.amount).toFixed(2);
    const expiresAtMs = new Date(session.expiresAt).getTime();

    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Pay - VELORA Premium</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
      background: #050505;
      color: #fff;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .container {
      max-width: 420px;
      width: 100%;
      padding: 24px 20px 40px;
    }
    .logo {
      text-align: center;
      padding: 20px 0 8px;
    }
    .logo h1 {
      font-size: 22px;
      font-weight: 800;
      letter-spacing: 4px;
      color: #D4AF37;
    }
    .logo span { color: #fff; font-size: 11px; display: block; margin-top: 2px; letter-spacing: 1px; opacity: 0.5; }
    .card {
      background: #111;
      border: 1px solid #222;
      border-radius: 16px;
      padding: 24px;
      margin-top: 20px;
    }
    .plan-info {
      text-align: center;
      margin-bottom: 20px;
    }
    .plan-name { font-size: 14px; color: #999; letter-spacing: 1px; text-transform: uppercase; }
    .plan-amount {
      font-size: 40px;
      font-weight: 800;
      color: #D4AF37;
      margin: 8px 0 4px;
    }
    .plan-amount span { font-size: 18px; vertical-align: top; margin-right: 2px; }
    .qr-container {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      display: flex;
      justify-content: center;
      align-items: center;
      margin: 16px auto;
      width: fit-content;
    }
    .qr-container img { width: 220px; height: 220px; display: block; }
    .scan-text {
      text-align: center;
      font-size: 13px;
      color: #888;
      margin: 12px 0 20px;
    }
    .upi-btn {
      display: block;
      width: 100%;
      padding: 16px;
      background: linear-gradient(135deg, #D4AF37, #B8962E);
      color: #000;
      font-size: 16px;
      font-weight: 700;
      border: none;
      border-radius: 12px;
      cursor: pointer;
      text-align: center;
      text-decoration: none;
      transition: transform 0.15s, box-shadow 0.15s;
    }
    .upi-btn:active { transform: scale(0.97); }
    .upi-btn:hover { box-shadow: 0 4px 20px rgba(212,175,55,0.3); }
    .timer {
      text-align: center;
      margin: 20px 0 16px;
      font-size: 15px;
      color: #ccc;
    }
    .timer-value {
      font-weight: 700;
      color: #D4AF37;
      font-size: 22px;
      font-variant-numeric: tabular-nums;
    }
    .timer-warn { color: #ff4444; }
    .divider {
      height: 1px;
      background: #222;
      margin: 20px 0;
    }
    .instructions {
      padding: 0 4px;
    }
    .instructions h3 {
      font-size: 13px;
      color: #888;
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 12px;
    }
    .step {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      margin-bottom: 12px;
    }
    .step-num {
      width: 24px;
      height: 24px;
      background: #1a1a1a;
      border: 1px solid #333;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 700;
      color: #D4AF37;
      flex-shrink: 0;
    }
    .step-text { font-size: 14px; color: #bbb; line-height: 1.5; padding-top: 2px; }
    .confirm-btn {
      display: block;
      width: 100%;
      padding: 16px;
      background: transparent;
      color: #D4AF37;
      font-size: 15px;
      font-weight: 600;
      border: 2px solid #D4AF37;
      border-radius: 12px;
      cursor: pointer;
      margin-top: 20px;
      transition: all 0.2s;
    }
    .confirm-btn:hover { background: rgba(212,175,55,0.1); }
    .confirm-btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .meta {
      margin-top: 20px;
      padding: 16px;
      background: #0a0a0a;
      border-radius: 10px;
      font-size: 12px;
      color: #666;
    }
    .meta-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
    .meta-row:last-child { margin-bottom: 0; }
    .meta-label { color: #555; }
    .meta-value { color: #888; font-family: monospace; font-size: 11px; word-break: break-all; }
    /* States */
    .state { display: none; text-align: center; padding: 48px 24px; }
    .state.active { display: block; }
    .state-icon { font-size: 56px; margin-bottom: 20px; }
    .state-title { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
    .state-desc { font-size: 14px; color: #888; line-height: 1.6; }
    .spinner {
      width: 48px; height: 48px;
      border: 3px solid #222;
      border-top-color: #D4AF37;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 20px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .success-icon {
      width: 72px; height: 72px;
      background: linear-gradient(135deg, #D4AF37, #B8962E);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 20px;
    }
    .success-icon svg { width: 36px; height: 36px; }
    .open-app-btn {
      display: inline-block;
      padding: 16px 40px;
      background: linear-gradient(135deg, #D4AF37, #B8962E);
      color: #000;
      font-size: 16px;
      font-weight: 700;
      border: none;
      border-radius: 12px;
      cursor: pointer;
      text-decoration: none;
      margin-top: 24px;
      transition: transform 0.15s;
    }
    .open-app-btn:active { transform: scale(0.97); }
    .expired-icon {
      width: 72px; height: 72px;
      background: #1a1a1a;
      border: 2px solid #333;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 20px;
      font-size: 32px;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .animate-in { animation: fadeIn 0.4s ease-out; }
    .pulse { animation: pulse 2s ease-in-out infinite; }
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">
      <h1>VELORA</h1>
      <span>PREMIUM</span>
    </div>

    <!-- PAYMENT STATE -->
    <div id="payment-state">
      <div class="card animate-in">
        <div class="plan-info">
          <div class="plan-name">${this.escapeHtml(session.planName)}</div>
          <div class="plan-amount"><span>\u20b9</span>${amount}</div>
        </div>

        <div class="qr-container">
          <img src="${qrUrl}" alt="Scan to Pay" id="qr-img"
               onerror="this.parentElement.innerHTML='<div style=\\'padding:40px;color:#999;font-size:13px\\'>QR unavailable. Use button below.</div>'" />
        </div>
        <div class="scan-text">Scan QR code with any UPI app</div>

        <a href="${this.escapeHtml(session.upiLink)}" class="upi-btn" id="pay-btn">
          Pay \u20b9${amount} via UPI
        </a>

        <div class="timer">
          <div>Session expires in</div>
          <div class="timer-value" id="timer">05:00</div>
        </div>

        <div class="divider"></div>

        <div class="instructions">
          <h3>How to pay</h3>
          <div class="step">
            <div class="step-num">1</div>
            <div class="step-text">Scan the QR code or tap the pay button above</div>
          </div>
          <div class="step">
            <div class="step-num">2</div>
            <div class="step-text">Complete the payment in your UPI app</div>
          </div>
          <div class="step">
            <div class="step-num">3</div>
            <div class="step-text">Come back here and tap "I've Completed Payment"</div>
          </div>
        </div>

        <button class="confirm-btn" id="confirm-btn" onclick="confirmPayment()">
          I've Completed Payment
        </button>

        <div class="meta">
          <div class="meta-row"><span class="meta-label">UPI ID</span><span class="meta-value">${this.escapeHtml(session.upiId)}</span></div>
          <div class="meta-row"><span class="meta-label">Payment ID</span><span class="meta-value">${this.escapeHtml(session.paymentId)}</span></div>
        </div>
      </div>
    </div>

    <!-- CONFIRMING STATE -->
    <div id="confirming-state" class="state">
      <div class="card animate-in">
        <div style="padding: 32px 0">
          <div class="spinner"></div>
          <div class="state-title">Verifying Payment</div>
          <div class="state-desc">
            Please wait while we confirm your transaction.<br />
            <span class="pulse" style="color:#D4AF37; display: inline-block; margin-top: 12px;">This may take a moment...</span>
          </div>
        </div>
      </div>
    </div>

    <!-- SUCCESS STATE -->
    <div id="success-state" class="state">
      <div class="card animate-in">
        <div style="padding: 32px 0">
          <div class="success-icon">
            <svg fill="none" stroke="#000" stroke-width="3" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/></svg>
          </div>
          <div class="state-title" style="color:#D4AF37">Payment Successful!</div>
          <div class="state-desc">
            Your premium plan has been activated.<br />
            Enjoy unlimited streaming!
          </div>
          <a href="velora://payment-success" class="open-app-btn">Open VELORA App</a>
          <div style="margin-top: 12px; font-size: 12px; color: #666;">
            Or simply switch back to the app
          </div>
        </div>
      </div>
    </div>

    <!-- EXPIRED STATE -->
    <div id="expired-state" class="state">
      <div class="card animate-in">
        <div style="padding: 32px 0">
          <div class="expired-icon">\u23f0</div>
          <div class="state-title" style="color:#ff4444">Session Expired</div>
          <div class="state-desc">
            This payment session has expired.<br />
            Please go back to the app and try again.
          </div>
          <a href="velora://payment-expired" class="open-app-btn" style="background:#333; color:#fff; margin-top:24px;">
            Back to App
          </a>
        </div>
      </div>
    </div>

    <!-- ERROR STATE -->
    <div id="error-state" class="state">
      <div class="card animate-in">
        <div style="padding: 32px 0">
          <div class="expired-icon">\u26a0\ufe0f</div>
          <div class="state-title" style="color:#ff4444">Something went wrong</div>
          <div class="state-desc" id="error-msg">
            An error occurred. Please try again from the app.
          </div>
          <a href="velora://payment-error" class="open-app-btn" style="background:#333; color:#fff; margin-top:24px;">
            Back to App
          </a>
        </div>
      </div>
    </div>
  </div>

  <script>
    var PAYMENT_ID = '${this.escapeHtml(session.paymentId)}';
    var EXPIRES_AT = ${expiresAtMs};
    var API_BASE = window.location.origin + '/api/v1/upi-payment';
    var pollInterval = null;
    var currentState = 'payment';

    // Timer
    function updateTimer() {
      var now = Date.now();
      var diff = EXPIRES_AT - now;
      if (diff <= 0) {
        showState('expired');
        return;
      }
      var mins = Math.floor(diff / 60000);
      var secs = Math.floor((diff % 60000) / 1000);
      var el = document.getElementById('timer');
      el.textContent = String(mins).padStart(2, '0') + ':' + String(secs).padStart(2, '0');
      if (diff < 60000) el.classList.add('timer-warn');
    }
    setInterval(updateTimer, 1000);
    updateTimer();

    function showState(state) {
      if (currentState === state) return;
      currentState = state;
      ['payment', 'confirming', 'success', 'expired', 'error'].forEach(function(s) {
        var el = document.getElementById(s + '-state');
        if (el) {
          el.classList.toggle('active', s === state);
          el.style.display = s === state ? 'block' : 'none';
        }
      });
      if (state !== 'confirming') stopPolling();
    }

    function startPolling() {
      if (pollInterval) return;
      pollInterval = setInterval(checkStatus, 3000);
    }

    function stopPolling() {
      if (pollInterval) { clearInterval(pollInterval); pollInterval = null; }
    }

    function checkStatus() {
      fetch(API_BASE + '/session-status/' + PAYMENT_ID)
        .then(function(r) { return r.json(); })
        .then(function(data) {
          if (data.status === 'verified') showState('success');
          else if (data.status === 'expired') showState('expired');
          else if (data.status === 'rejected') {
            document.getElementById('error-msg').textContent = 'Payment was not verified. Please contact support.';
            showState('error');
          }
        })
        .catch(function() { /* silent retry */ });
    }

    function confirmPayment() {
      var btn = document.getElementById('confirm-btn');
      btn.disabled = true;
      btn.textContent = 'Confirming...';

      fetch(API_BASE + '/confirm-payment/' + PAYMENT_ID, { method: 'POST' })
        .then(function(r) { return r.json(); })
        .then(function(data) {
          if (data.status === 'success') {
            showState('success');
          } else if (data.status === 'confirming') {
            showState('confirming');
            startPolling();
          } else if (data.statusCode === 400) {
            document.getElementById('error-msg').textContent = data.message || 'Payment failed';
            showState('error');
          }
        })
        .catch(function(err) {
          document.getElementById('error-msg').textContent = 'Network error. Please check your connection.';
          showState('error');
        });
    }

    // Init: show payment state
    document.getElementById('payment-state').style.display = 'block';
  </script>
</body>
</html>`;
  }

  private renderSuccessPage(session: any): string {
    return `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Payment Successful - VELORA</title>
<style>
  body { font-family: -apple-system, system-ui, sans-serif; background: #050505; color: #fff; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; }
  .box { text-align: center; padding: 40px 24px; max-width: 400px; }
  .icon { width: 72px; height: 72px; background: linear-gradient(135deg, #D4AF37, #B8962E); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 24px; }
  .icon svg { width: 36px; height: 36px; }
  h1 { color: #D4AF37; font-size: 24px; margin-bottom: 8px; }
  p { color: #888; font-size: 14px; line-height: 1.6; }
  a { display: inline-block; margin-top: 24px; padding: 16px 40px; background: linear-gradient(135deg, #D4AF37, #B8962E); color: #000; font-weight: 700; border-radius: 12px; text-decoration: none; }
</style></head><body>
<div class="box">
  <div class="icon"><svg fill="none" stroke="#000" stroke-width="3" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/></svg></div>
  <h1>Payment Successful!</h1>
  <p>${this.escapeHtml(session.planName)} has been activated.</p>
  <a href="velora://payment-success">Open VELORA App</a>
</div></body></html>`;
  }

  private renderExpiredPage(session: any): string {
    return `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Session Expired - VELORA</title>
<style>
  body { font-family: -apple-system, system-ui, sans-serif; background: #050505; color: #fff; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; }
  .box { text-align: center; padding: 40px 24px; max-width: 400px; }
  .icon { font-size: 56px; margin-bottom: 24px; }
  h1 { color: #ff4444; font-size: 22px; margin-bottom: 8px; }
  p { color: #888; font-size: 14px; line-height: 1.6; }
  a { display: inline-block; margin-top: 24px; padding: 16px 40px; background: #333; color: #fff; font-weight: 700; border-radius: 12px; text-decoration: none; }
</style></head><body>
<div class="box">
  <div class="icon">\u23f0</div>
  <h1>Session Expired</h1>
  <p>This payment session has expired. Please try again from the app.</p>
  <a href="velora://payment-expired">Back to App</a>
</div></body></html>`;
  }

  private renderErrorPage(message: string): string {
    return `<!DOCTYPE html>
<html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Error - VELORA</title>
<style>
  body { font-family: -apple-system, system-ui, sans-serif; background: #050505; color: #fff; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; }
  .box { text-align: center; padding: 40px 24px; max-width: 400px; }
  .icon { font-size: 56px; margin-bottom: 24px; }
  h1 { color: #ff4444; font-size: 22px; margin-bottom: 8px; }
  p { color: #888; font-size: 14px; line-height: 1.6; }
  a { display: inline-block; margin-top: 24px; padding: 16px 40px; background: #333; color: #fff; font-weight: 700; border-radius: 12px; text-decoration: none; }
</style></head><body>
<div class="box">
  <div class="icon">\u26a0\ufe0f</div>
  <h1>Error</h1>
  <p>${this.escapeHtml(message)}</p>
  <a href="velora://payment-error">Back to App</a>
</div></body></html>`;
  }

  private escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }
}
