const https = require('https');
const http = require('http');

function httpRequest(url, method = 'GET', postData = null, extraHeaders = {}) {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url);
    const mod = urlObj.protocol === 'https:' ? https : http;
    const options = {
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + urlObj.search,
      method,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
        ...extraHeaders
      }
    };
    if (postData) {
      options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
      options.headers['Content-Length'] = Buffer.byteLength(postData);
    }
    const req = mod.request(options, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        const loc = res.headers.location.startsWith('http') ? res.headers.location : new URL(res.headers.location, url).href;
        return httpRequest(loc, 'GET', null, extraHeaders).then(resolve).catch(reject);
      }
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve({ body: Buffer.concat(chunks).toString(), headers: res.headers, status: res.statusCode }));
    });
    req.on('error', reject);
    if (postData) req.write(postData);
    req.end();
  });
}

async function main() {
  const fileCode = 'pcwv2guza7gi';
  const baseUrl = 'https://hgcloud.to';

  // Approach 1: POST to embed URL (common FileLions/StreamWish pattern)
  console.log('=== Approach 1: POST to /e/ ===');
  const formData = `op=download_orig&id=${fileCode}&mode=o&hash=`;
  const r1 = await httpRequest(`${baseUrl}/e/${fileCode}`, 'POST', formData, {
    'Referer': `${baseUrl}/e/${fileCode}`,
    'Origin': baseUrl
  });
  console.log('Status:', r1.status, 'Length:', r1.body.length);
  findUrls(r1.body, 'Approach 1');

  // Approach 2: POST with different op
  console.log('\n=== Approach 2: POST download2 ===');
  const formData2 = `op=download2&id=${fileCode}&rand=&referer=&method_free=&method_premium=`;
  const r2 = await httpRequest(`${baseUrl}/e/${fileCode}`, 'POST', formData2, {
    'Referer': `${baseUrl}/e/${fileCode}`,
    'Origin': baseUrl
  });
  console.log('Status:', r2.status, 'Length:', r2.body.length);
  findUrls(r2.body, 'Approach 2');

  // Approach 3: Try /d/ direct download endpoint
  console.log('\n=== Approach 3: /d/ endpoint ===');
  const r3 = await httpRequest(`${baseUrl}/d/${fileCode}`, 'GET', null, {
    'Referer': `${baseUrl}/e/${fileCode}`
  });
  console.log('Status:', r3.status, 'Length:', r3.body.length);
  findUrls(r3.body, 'Approach 3');

  // Approach 4: Try /download/ endpoint
  console.log('\n=== Approach 4: /download/ endpoint ===');
  const r4 = await httpRequest(`${baseUrl}/download/${fileCode}`, 'GET', null, {
    'Referer': `${baseUrl}/e/${fileCode}`
  });
  console.log('Status:', r4.status, 'Length:', r4.body.length);
  findUrls(r4.body, 'Approach 4');

  // Approach 5: Try direct file URL pattern used by StreamWish
  console.log('\n=== Approach 5: StreamWish API pattern ===');
  const r5 = await httpRequest(`${baseUrl}/${fileCode}`, 'GET', null, {
    'Referer': `${baseUrl}`
  });
  console.log('Status:', r5.status, 'Length:', r5.body.length);
  findUrls(r5.body, 'Approach 5');
  
  // Show body snippets for approaches that returned something different
  if (r1.body.length > 1000) {
    console.log('\n=== Approach 1 body (first 3000 chars) ===');
    console.log(r1.body.substring(0, 3000));
  }
  if (r3.body.length > 1000 && r3.body !== r1.body) {
    console.log('\n=== Approach 3 body (first 3000 chars) ===');
    console.log(r3.body.substring(0, 3000));
  }
  if (r5.body.length > 1000 && r5.body !== r1.body) {
    console.log('\n=== Approach 5 body (first 3000 chars) ===');
    console.log(r5.body.substring(0, 3000));
  }
}

function findUrls(html, label) {
  const m3u8 = html.match(/https?:\/\/[^'"\\s]+\.m3u8[^'"\\s]*/g);
  if (m3u8) { console.log(`[${label}] M3U8:`, m3u8); return; }
  
  const mp4 = html.match(/https?:\/\/[^'"\\s]+\.mp4[^'"\\s]*/g);
  if (mp4) { console.log(`[${label}] MP4:`, mp4); return; }
  
  const videoUrls = html.match(/https?:\/\/[^'"\\s]*(?:video|stream|cdn|hls|play)[^'"\\s]*/g);
  if (videoUrls) { console.log(`[${label}] Video URLs:`, [...new Set(videoUrls)]); return; }
  
  const file = html.match(/["']?file["']?\s*[:=]\s*["']([^"']+)["']/);
  if (file) { console.log(`[${label}] file:`, file[1]); return; }
  
  const sources = html.match(/sources\s*[:=]\s*\[([^\]]+)\]/);
  if (sources) { console.log(`[${label}] sources:`, sources[1]); return; }

  const packed = html.match(/eval\(function\(p,a,c,k,e/);
  if (packed) { console.log(`[${label}] PACKED JS found - contains video URL`); return; }

  console.log(`[${label}] No video URL found`);
}

main().catch(console.error);
