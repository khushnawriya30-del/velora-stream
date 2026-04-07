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
    const firebaseApiKey =
      this.configService.get<string>('FIREBASE_WEB_API_KEY') || 'AIzaSyCYm3w06LQbfFGqCiGYSBYfbExNokEIJaw';
    const baseUrl = this.configService.get<string>('BASE_URL') || '';
    const apiPrefix = this.configService.get<string>('API_PREFIX') || 'api/v1';
    const apiBase = baseUrl.includes(apiPrefix)
      ? baseUrl
      : `${baseUrl.replace(/\/$/, '')}/${apiPrefix}`;
    const authMode = mode === 'signup' ? 'signup' : 'login';

    this.logger.log(`Google web sign-in page rendered (mode: ${authMode})`);
    return res.send(this.renderHTML(firebaseApiKey, apiBase, authMode));
  }

  private renderHTML(
    firebaseApiKey: string,
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
             background: rgba(255,82,82,0.1); border-radius: 8px; display: none; }
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

  <script src="https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js"></script>
  <script src="https://www.gstatic.com/firebasejs/10.12.0/firebase-auth-compat.js"></script>
  <script>
    firebase.initializeApp({
      apiKey: '${firebaseApiKey}',
      authDomain: 'velora-a97e2.firebaseapp.com',
      projectId: 'velora-a97e2',
    });
    const auth = firebase.auth();
    const provider = new firebase.auth.GoogleAuthProvider();
    provider.addScope('email');
    provider.addScope('profile');
    // Force account picker — show ALL Google accounts, never auto-select
    provider.setCustomParameters({ prompt: 'select_account' });

    const API_BASE = '${apiBase}'.replace(/\\/$/, '');
    const AUTH_MODE = '${mode}';

    // Check for redirect result on page load
    auth.getRedirectResult().then(result => {
      if (result && result.user) {
        handleFirebaseUser(result);
      }
    }).catch(err => {
      showError(err.message || 'Sign-in redirect failed');
    });

    function startGoogleSignIn() {
      document.getElementById('googleBtn').disabled = true;
      document.getElementById('loading').classList.add('show');
      document.getElementById('status').textContent = 'Redirecting to Google...';
      auth.signInWithRedirect(provider);
    }

    async function handleFirebaseUser(result) {
      document.getElementById('googleBtn').style.display = 'none';
      document.getElementById('loading').classList.add('show');
      document.getElementById('status').textContent = 'Verifying...';

      try {
        // Get the Google ID token from the OAuthCredential
        const credential = firebase.auth.GoogleAuthProvider.credentialFromResult(result);
        let idToken = credential ? credential.idToken : null;

        // Fallback: use Firebase ID token if Google credential not available
        if (!idToken && result.user) {
          idToken = await result.user.getIdToken(true);
        }
        if (!idToken) throw new Error('Could not get authentication token');

        // Determine endpoint based on token source
        let endpoint;
        if (credential && credential.idToken) {
          // We have a raw Google ID token — use the google/mobile endpoint
          endpoint = AUTH_MODE === 'signup'
            ? API_BASE + '/auth/google/mobile/signup'
            : API_BASE + '/auth/google/mobile';
        } else {
          // We have a Firebase ID token — use the firebase phone/verify endpoint
          // Actually, let's just get the Google ID token from Firebase user info
          // and build a synthetic request to the google mobile endpoint
          endpoint = AUTH_MODE === 'signup'
            ? API_BASE + '/auth/google/mobile/signup'
            : API_BASE + '/auth/google/mobile';
        }

        const resp = await fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ idToken }),
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
