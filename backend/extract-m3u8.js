const https = require('https');

const embedUrl = 'https://hgcloud.to/e/pcwv2guza7gi';

function fetch(url) {
  return new Promise((resolve, reject) => {
    const mod = url.startsWith('https') ? https : require('http');
    mod.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://hgcloud.to/',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
      }
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return fetch(res.headers.location).then(resolve).catch(reject);
      }
      let data = '';
      res.on('data', c => data += c);
      res.on('end', () => resolve(data));
    }).on('error', reject);
  });
}

async function extract() {
  console.log('Fetching embed page...');
  const html = await fetch(embedUrl);
  console.log('Page length:', html.length);
  
  // 1. Direct m3u8 URL
  const m3u8 = html.match(/https?:\/\/[^'"]+\.m3u8[^'"\\]*/g);
  if (m3u8) {
    console.log('\n=== M3U8 URLs Found ===');
    m3u8.forEach(u => console.log(u));
  }

  // 2. Check for file/sources
  const fileMatch = html.match(/["']?file["']?\s*[:=]\s*["']([^"']+)["']/);
  if (fileMatch) console.log('\nFILE:', fileMatch[1]);
  
  const srcMatch = html.match(/sources\s*[:=]\s*\[([^\]]+)\]/);
  if (srcMatch) console.log('\nSOURCES:', srcMatch[1]);

  // 3. Check for packed JS (eval)
  const packed = html.match(/eval\(function\(p,a,c,k,e,[dr]\)\{.*?\}\('([^']+)'/s);
  if (packed) {
    console.log('\n=== PACKED JS FOUND - Unpacking... ===');
    try {
      // Simple p.a.c.k.e.r unpacker
      const evalCode = html.match(/eval\(function\(p,a,c,k,e,[dr]\)\{.*?\}\(.*?\)\)/s);
      if (evalCode) {
        const unpacked = eval(evalCode[0].replace('eval(', '('));
        console.log('Unpacked length:', unpacked.length);
        const m3u8Inner = unpacked.match(/https?:\/\/[^'"]+\.m3u8[^'"\\]*/g);
        if (m3u8Inner) {
          console.log('\n=== M3U8 from unpacked ===');
          m3u8Inner.forEach(u => console.log(u));
        }
        const fileInner = unpacked.match(/["']?file["']?\s*[:=]\s*["']([^"']+)["']/);
        if (fileInner) console.log('FILE from unpacked:', fileInner[1]);
        const srcInner = unpacked.match(/sources\s*[:=]\s*\[([^\]]+)\]/);
        if (srcInner) console.log('SOURCES from unpacked:', srcInner[1]);
      }
    } catch (e) {
      console.log('Unpack error:', e.message);
    }
  }

  // 4. Look for any video URLs
  const videoUrls = html.match(/https?:\/\/[^'"]*(?:\.mp4|\.mkv|\.m3u8|master\.m3u8|index\.m3u8)[^'"\\]*/g);
  if (videoUrls) {
    console.log('\n=== Video URLs Found ===');
    videoUrls.forEach(u => console.log(u));
  }

  // 5. Show all inline scripts for manual inspection
  const scripts = html.match(/<script(?:\s[^>]*)?>[\s\S]*?<\/script>/gi) || [];
  console.log('\n=== Inline Scripts Summary ===');
  scripts.forEach((s, i) => {
    const content = s.replace(/<\/?script[^>]*>/gi, '').trim();
    if (content.length > 20) {
      console.log(`\nScript ${i} (${content.length} chars):`);
      // Show first 600 chars
      console.log(content.substring(0, 600));
      if (content.length > 600) console.log('...[truncated]');
    }
  });
}

extract().catch(console.error);
