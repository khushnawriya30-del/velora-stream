const GITHUB_OWNER = 'khushnawriya30-del';
const GITHUB_REPO = 'velora-stream';

export const config = {
  runtime: 'edge',
};

export default async function handler(_req: Request): Promise<Response> {
  try {
    // Fetch latest release from GitHub Releases API
    const res = await fetch(
      `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`,
      {
        headers: {
          Accept: 'application/vnd.github.v3+json',
          'User-Agent': 'VELORA-Website',
        },
      },
    );

    if (res.ok) {
      const release = await res.json() as { assets: Array<{ name: string; browser_download_url: string }> };
      const apk = release.assets?.find((a) => a.name.endsWith('.apk'));
      if (apk?.browser_download_url) {
        return new Response(null, {
          status: 302,
          headers: {
            Location: apk.browser_download_url,
            'Cache-Control': 'no-store, no-cache, must-revalidate',
            'Content-Disposition': `attachment; filename="${apk.name}"`,
          },
        });
      }
    }

    // Fallback: redirect to GitHub releases page
    return new Response(null, {
      status: 302,
      headers: {
        Location: `https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`,
        'Cache-Control': 'no-store',
      },
    });
  } catch {
    return new Response(null, {
      status: 302,
      headers: {
        Location: `https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`,
      },
    });
  }
}

