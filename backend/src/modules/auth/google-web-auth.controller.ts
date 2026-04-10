import { Controller, Get, Post, Res, Body, Logger, Query, Req } from '@nestjs/common';
import { Request, Response } from 'express';
import { ConfigService } from '@nestjs/config';
import { AuthService } from './auth.service';

@Controller('auth/google-web')
export class GoogleWebAuthController {
  private readonly logger = new Logger(GoogleWebAuthController.name);
  private readonly googleClientId = '134822516206-19l2pi0afo6450c3gksg3be1404saksi.apps.googleusercontent.com';

  constructor(
    private readonly configService: ConfigService,
    private readonly authService: AuthService,
  ) {}

  /**
   * Step 1: Redirect to Google OAuth (implicit flow — response_type=token).
   * Google returns the access_token directly in the URL fragment.
   */
  @Get()
  renderGoogleSignInPage(
    @Query('mode') mode: string,
    @Req() req: Request,
    @Res() res: Response,
  ) {
    const authMode = mode === 'signup' ? 'signup' : 'login';
    const protocol = req.headers['x-forwarded-proto'] || 'https';
    const host = req.headers['x-forwarded-host'] || req.headers.host;
    const redirectUri = `${protocol}://${host}/auth/google-web/callback`;

    const params = new URLSearchParams({
      client_id: this.googleClientId,
      redirect_uri: redirectUri,
      response_type: 'token',
      scope: 'openid email profile',
      prompt: 'select_account',
      state: authMode,
    });

    this.logger.log(`Redirecting to Google OAuth implicit flow (mode: ${authMode})`);
    return res.redirect(`https://accounts.google.com/o/oauth2/v2/auth?${params.toString()}`);
  }

  /**
   * Step 2: Google redirects here with the access_token in the URL fragment (#access_token=...).
   * Fragments are not sent to the server, so we serve an HTML page that reads
   * the fragment client-side, sends the token to /auth/google-web/verify,
   * then redirects to the velora:// deep link.
   */
  @Get('callback')
  handleGoogleCallback(@Res() res: Response) {
    return res.send(this.renderCallbackHTML());
  }

  /**
   * Step 3: Client-side JS POSTs the access_token here.
   * We verify it, create/find the user, and return JWT tokens.
   */
  @Post('verify')
  async verifyGoogleToken(
    @Body() body: { accessToken: string },
    @Req() req: Request,
    @Res() res: Response,
  ) {
    try {
      if (!body.accessToken) {
        return res.status(400).json({ message: 'Missing accessToken' });
      }

      const ipAddress = (req.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim()
        || req.socket?.remoteAddress
        || 'unknown';
      const result = await this.authService.googleVerifyAccessToken(body.accessToken, undefined, ipAddress);
      this.logger.log(`Google OAuth verify success for user ${result.user?.email}`);
      return res.json(result);
    } catch (err) {
      this.logger.error(`Google OAuth verify error: ${err.message}`);
      return res.status(401).json({ message: err.message || 'Authentication failed' });
    }
  }

  /** HTML page served at /callback — parses the fragment and completes sign-in */
  private renderCallbackHTML(): string {
    return `<!DOCTYPE html>
<html><head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>VELORA - Signing In</title>
<style>
  body { font-family: -apple-system, system-ui, sans-serif; background: #050505; color: #fff;
         min-height: 100vh; display: flex; align-items: center; justify-content: center; }
  .c { text-align: center; padding: 24px; max-width: 380px; }
  .logo { font-size: 28px; font-weight: 800; letter-spacing: 2px;
          background: linear-gradient(135deg, #C5A44E, #F2D078, #C5A44E);
          -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 16px; }
  .msg { color: #aaa; font-size: 16px; margin-bottom: 24px; }
  .spinner { width: 36px; height: 36px; border: 3px solid #333; border-top-color: #C5A44E;
             border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 20px; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .err { color: #ff5252; font-size: 14px; padding: 12px; background: rgba(255,82,82,0.1);
         border-radius: 8px; margin-bottom: 20px; display: none; word-break: break-word; }
  .link { color: #C5A44E; text-decoration: underline; font-size: 14px; display: none; }
  .btn { padding: 12px 32px; background: linear-gradient(135deg, #C5A44E, #F2D078); color: #000;
         border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer;
         display: none; margin-top: 12px; }
</style>
</head><body>
<div class="c">
  <div class="logo">VELORA</div>
  <div class="spinner" id="spinner"></div>
  <div class="msg" id="msg">Completing sign in...</div>
  <div class="err" id="err"></div>
  <a class="link" id="link" href="#">Tap here if not redirected</a>
  <button class="btn" id="retryBtn" onclick="location.href=location.origin+'/auth/google-web'">Try Again</button>
</div>
<script>
(async function() {
  var msgEl = document.getElementById('msg');
  var errEl = document.getElementById('err');
  var spinnerEl = document.getElementById('spinner');
  var linkEl = document.getElementById('link');
  var retryBtn = document.getElementById('retryBtn');

  function showError(m) {
    spinnerEl.style.display = 'none';
    msgEl.style.display = 'none';
    errEl.textContent = m;
    errEl.style.display = 'block';
    retryBtn.style.display = 'inline-block';
  }

  // Parse access_token from URL fragment
  var hash = window.location.hash.substring(1);
  if (!hash) { showError('No authentication data received. Please try again.'); return; }
  var params = new URLSearchParams(hash);
  var accessToken = params.get('access_token');
  var error = params.get('error');
  if (error) { showError(error); return; }
  if (!accessToken) { showError('No access token received. Please try again.'); return; }

  try {
    msgEl.textContent = 'Verifying your account...';

    // Send to backend for verification
    var resp = await fetch(location.origin + '/auth/google-web/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accessToken: accessToken })
    });
    if (!resp.ok) {
      var err = await resp.json().catch(function() { return {}; });
      throw new Error(err.message || 'Server error ' + resp.status);
    }
    var data = await resp.json();

    // Build deep link
    var dlParams = new URLSearchParams({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      user: JSON.stringify(data.user)
    });
    var deepLink = 'velora://auth-callback?' + dlParams.toString();

    msgEl.textContent = 'Signed in! Returning to app...';
    linkEl.href = deepLink;
    linkEl.style.display = 'inline';

    window.location.href = deepLink;
  } catch(e) {
    showError(e.message || 'Something went wrong');
  }
})();
</script>
</body></html>`;
  }
}
