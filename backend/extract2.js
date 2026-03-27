// Extract m3u8 from HGCloud using deobfuscation approach
const https = require('https');
const http = require('http');

function httpGet(url, extraHeaders = {}) {
  return new Promise((resolve, reject) => {
    const mod = url.startsWith('https') ? https : http;
    mod.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': '*/*',
        'Accept-Language': 'en-US,en;q=0.9',
        ...extraHeaders
      }
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        const loc = res.headers.location.startsWith('http') ? res.headers.location : new URL(res.headers.location, url).href;
        return httpGet(loc, extraHeaders).then(resolve).catch(reject);
      }
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve({ body: Buffer.concat(chunks).toString(), headers: res.headers, status: res.statusCode }));
    }).on('error', reject);
  });
}

async function main() {
  // Step 1: Fetch main.js
  console.log('Fetching main.js...');
  const { body: js } = await httpGet('https://hgcloud.to/main.js?v=1.1.8', { 'Referer': 'https://hgcloud.to/e/pcwv2guza7gi' });
  console.log('main.js length:', js.length);

  // Extract string literals that look like URLs or paths (decoded from the obfuscation)
  // Look for fetch/XMLHttpRequest patterns
  const fetchPatterns = js.match(/fetch\s*\(/g) || [];
  console.log('fetch() calls:', fetchPatterns.length);

  const xhrPatterns = js.match(/XMLHttpRequest|\.open\s*\(/g) || [];
  console.log('XHR patterns:', xhrPatterns.length);

  // Look for string array used by the obfuscator
  const strArray = js.match(/_0x[a-f0-9]+\s*=\s*\[([^\]]{100,})\]/);
  if (strArray) {
    console.log('\nString array found, extracting...');
    try {
      const strings = eval('[' + strArray[1] + ']');
      const interesting = strings.filter(s => 
        typeof s === 'string' && (
          s.includes('m3u8') || s.includes('http') || s.includes('.mp4') ||
          s.includes('file') || s.includes('source') || s.includes('video') ||
          s.includes('hls') || s.includes('stream') || s.includes('play') ||
          s.includes('/e/') || s.includes('/d/') || s.includes('/api') ||
          s.includes('fetch') || s.includes('xhr') || s.includes('master') ||
          s.includes('.to') || s.includes('cdn')
        )
      );
      console.log('Interesting strings:');
      interesting.forEach(s => console.log(' ', s));
    } catch(e) {
      console.log('Could not eval string array:', e.message);
    }
  }

  // Try to find the string rotation/decode function and decode all strings
  // Look for the _0x#### function pattern
  const decodeFn = js.match(/function\s+(_0x[a-f0-9]+)\s*\(\s*_0x[a-f0-9]+\s*,\s*_0x[a-f0-9]+\s*\)\s*\{\s*(?:const|var|let)\s+_0x[a-f0-9]+\s*=\s*_0x[a-f0-9]+\s*\(\s*\)\s*;/);
  if (decodeFn) {
    console.log('\nDecode function found:', decodeFn[1]);
  }

  // Alternative: Look for URL patterns encoded in hex or unicode
  const hexUrls = js.match(/\\x68\\x74\\x74\\x70[^'"]+/g) || [];
  if (hexUrls.length) {
    console.log('\nHex-encoded URLs:');
    hexUrls.forEach(u => console.log(' ', u));
  }

  // Look for the initial array of strings
  const initArray = js.match(/(?:const|var)\s+(_0x[a-f0-9]+)\s*=\s*\(\s*function\s*\(\)\s*\{[\s\S]*?return\s+_0x[a-f0-9]+\s*;\s*\}\s*\(\s*\)\s*\)/);
  if (initArray) {
    console.log('\nInitial string array function found:', initArray[1]);
  }

  // Extract all string literals
  const allStrings = new Set();
  const strMatches = js.matchAll(/'([^']{3,})'/g);
  for (const m of strMatches) {
    if (m[1].includes('http') || m[1].includes('.m3u8') || m[1].includes('file') || 
        m[1].includes('/e/') || m[1].includes('source') || m[1].includes('fetch') ||
        m[1].includes('video') || m[1].includes('stream') || m[1].includes('hls') ||
        m[1].includes('.mp4') || m[1].includes('play') || m[1].includes('cdn') ||
        m[1].includes('POST') || m[1].includes('GET') || m[1].includes('json')) {
      allStrings.add(m[1]);
    }
  }
  if (allStrings.size) {
    console.log('\nInteresting string literals:');
    allStrings.forEach(s => console.log(' ', s));
  }

  // Look for path patterns in window.location parsing
  const pathMatch = js.match(/location\.pathname|window\.location|location\.href/g) || [];
  console.log('\nlocation references:', pathMatch.length);
}

main().catch(console.error);
