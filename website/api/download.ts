const BLOB_BASE = 'https://3upclzuvtnblwr5f.public.blob.vercel-storage.com';

export const config = {
  runtime: 'edge',
};

export default async function handler(_req: Request): Promise<Response> {
  try {
    // Fetch latest metadata from Vercel Blob CDN
    const metaRes = await fetch(`${BLOB_BASE}/velora-latest.json`, {
      headers: { 'Cache-Control': 'no-cache' },
    });

    if (metaRes.ok) {
      const meta = (await metaRes.json()) as { version: string; url: string; name: string };
      if (meta.url) {
        return new Response(null, {
          status: 302,
          headers: {
            Location: meta.url,
            'Cache-Control': 'no-store',
            'Content-Disposition': `attachment; filename="${meta.name || 'VELORA-latest.apk'}"`,
          },
        });
      }
    }

    // Hard fallback to direct APK blob
    return new Response(null, {
      status: 302,
      headers: {
        Location: `${BLOB_BASE}/VELORA-latest.apk`,
        'Cache-Control': 'no-store',
      },
    });
  } catch {
    return new Response(null, {
      status: 302,
      headers: { Location: `${BLOB_BASE}/VELORA-latest.apk` },
    });
  }
}

