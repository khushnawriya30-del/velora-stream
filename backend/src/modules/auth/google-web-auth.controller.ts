import { Controller, Get, Res, Logger, Query } from '@nestjs/common';
import { Response } from 'express';
import { ConfigService } from '@nestjs/config';

@Controller('auth/google-web')
export class GoogleWebAuthController {
  private readonly logger = new Logger(GoogleWebAuthController.name);

  constructor(private readonly configService: ConfigService) {}

  @Get()
  renderGoogleSignInPage(
    @Query('mode') mode: string,
    @Res() res: Response,
  ) {
    const googleClientId = '134822516206-19l2pi0afo6450c3gksg3be1404saksi.apps.googleusercontent.com';
    const baseUrl = this.configService.get<string>('BASE_URL') || '';
    const apiPrefix = this.configService.get<string>('API_PREFIX') || 'api/v1';
    const apiBase = baseUrl.includes(apiPrefix)
      ? baseUrl
      : `${baseUrl.replace(/\/$/, '')}/${apiPrefix}`;
    const authMode = mode === 'signup' ? 'signup' : 'login';

    this.logger.log(`Google web sign-in page rendered (mode: ${authMode})`);
    return res.send(this.renderHTML(googleClientId, apiBase, authMode));
  }

  private renderHTML(
    googleClientId: string,
    apiBase: string,
    mode: string,
  ): string {
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>VELORA - Sign in with Google</title>
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
      justify-content: center;
      padding: 24px;
    }
    .container { width: 100%; max-width: 380px; text-align: center; }
    .logo {
      font-size: 28px; font-weight: 800; letter-spacing: 2px;
      background: linear-gradient(135deg, #C5A44E, #F2D078, #C5A44E);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent;
      margin-bottom: 8px;
    }
    .subtitle { color: #888; font-size: 14px; margin-bottom: 40px; }
    .google-btn {
      display: inline-flex; align-items: center; gap: 12px;
      padding: 14px 32px; background: #fff; color: #1f1f1f;
      border: none; border-radius: 12px; font-size: 16px; font-weight: 600;
      cursor: pointer; transition: opacity 0.2s;
    }
    .google-btn:active { opacity: 0.8; }
    .google-btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .google-btn svg { width: 20px; height: 20px; }
    .status { color: #aaa; font-size: 14px; margin-top: 20px; min-height: 20px; }
    .error { color: #ff5252; font-size: 14px; margin-top: 16px; padding: 12px;
             background: rgba(255,82,82,0.1); border-radius: 8px; display: none;
             word-break: break-word; }
    .loading { display: none; margin-top: 24px; }
    .loading.show { display: block; }
    .spinner { width: 36px; height: 36px; border: 3px solid #333; border-top-color: #C5A44E;
               border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .retry-btn { display: none; margin-top: 20px; padding: 12px 32px;
                 background: linear-gradient(135deg, #C5A44E, #F2D078); color: #000;
                 border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">VELORA</div>
    <div class="subtitle">Sign in with your Google account</div>
    <button class="google-btn" id="googleBtn" onclick="startGoogleSignIn()">
      <svg viewBox="0 0 48 48"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/><path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/></svg>
      Continue with Google
    </button>
    <div class="loading" id="loading"><div class="spinner"></div><div class="status" id="status">Signing you in...</div></div>
    <div class="error" id="error"></div>
    <button class="retry-btn" id="retryBtn" onclick="location.reload()">Try Again</button>
  </div>

  <script src="https://accounts.google.com/gsi/client"></script>
  <script>
    const GOOGLE_CLIENT_ID = '${googleClientId}';
    const API_BASE = '${apiBase}'.replace(/\\/$/, '');
    const AUTH_MODE = '${mode}';
    let tokenClient = null;

    // Initialize GIS token client (popup mode — no redirect URI needed)
    function initGIS() {
      tokenClient = google.accounts.oauth2.initTokenClient({
        client_id: GOOGLE_CLIENT_ID,
        scope: 'email profile openid',
        prompt: 'select_account',
        callback: handleTokenResponse,
        error_callback: handleTokenError,
      });
    }

    // Wait for GIS library to load, then init
    if (typeof google !== 'undefined' && google.accounts) {
      initGIS();
    } else {
      window.addEventListener('load', () => {
        if (typeof google !== 'undefined' && google.accounts) initGIS();
        else showError('Failed to load Google Sign-In library');
      });
    }

    function startGoogleSignIn() {
      if (!tokenClient) {
        showError('Google Sign-In not ready. Please reload.');
        return;
      }
      document.getElementById('googleBtn').disabled = true;
      document.getElementById('loading').classList.add('show');
      document.getElementById('status').textContent = 'Opening Google Sign-In...';
      tokenClient.requestAccessToken();
    }

    async function handleTokenResponse(response) {
      if (response.error) {
        showError(response.error_description || response.error);
        return;
      }
      document.getElementById('googleBtn').style.display = 'none';
      document.getElementById('loading').classList.add('show');
      document.getElementById('status').textContent = 'Verifying your account...';

      try {
        const accessToken = response.access_token;

        // Get user info from Google
        const userInfoResp = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
          headers: { Authorization: 'Bearer ' + accessToken },
        });
        if (!userInfoResp.ok) throw new Error('Failed to get Google user info');
        const userInfo = await userInfoResp.json();

        // Get a Google ID token via tokeninfo
        const tokenInfoResp = await fetch('https://oauth2.googleapis.com/tokeninfo?access_token=' + encodeURIComponent(accessToken));
        const tokenInfo = await tokenInfoResp.json();

        // Send to our backend — use access token flow
        const endpoint = AUTH_MODE === 'signup'
          ? API_BASE + '/auth/google/web-signup'
          : API_BASE + '/auth/google/web-login';

        const resp = await fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            accessToken: accessToken,
            sub: userInfo.sub,
            email: userInfo.email,
            name: userInfo.name,
            picture: userInfo.picture,
          }),
        });
        if (!resp.ok) {
          const err = await resp.json().catch(() => ({}));
          throw new Error(err.message || 'Server error ' + resp.status);
        }
        const data = await resp.json();
        document.getElementById('status').textContent = 'Success! Redirecting...';

        const params = new URLSearchParams({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user: JSON.stringify(data.user),
        });
        window.location.href = 'velora://auth-callback?' + params.toString();
      } catch (err) {
        showError(err.message || 'Something went wrong');
      }
    }

    function handleTokenError(error) {
      if (error.type === 'popup_closed') {
        // User closed the popup — just reset UI
        document.getElementById('googleBtn').disabled = false;
        document.getElementById('loading').classList.remove('show');
        return;
      }
      showError(error.message || error.type || 'Google Sign-In failed');
    }

    function showError(msg) {
      document.getElementById('loading').classList.remove('show');
      document.getElementById('googleBtn').style.display = 'inline-flex';
      document.getElementById('googleBtn').disabled = false;
      document.getElementById('error').textContent = msg;
      document.getElementById('error').style.display = 'block';
      document.getElementById('retryBtn').style.display = 'inline-block';
    }
  </script>
</body>
</html>`;
  }
}
