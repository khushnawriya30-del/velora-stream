const puppeteer = require('puppeteer');

async function extractM3U8(embedUrl) {
  console.log('Launching browser...');
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  // Set user agent
  await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36');
  
  // Intercept network requests to find m3u8
  const foundUrls = [];
  
  page.on('request', (request) => {
    const url = request.url();
    if (url.includes('.m3u8') || url.includes('.mp4') || url.includes('.ts')) {
      console.log('[REQUEST]', request.method(), url.substring(0, 200));
      foundUrls.push({ type: 'request', url, method: request.method() });
    }
  });
  
  page.on('response', async (response) => {
    const url = response.url();
    const contentType = response.headers()['content-type'] || '';
    if (url.includes('.m3u8') || contentType.includes('mpegurl') || contentType.includes('m3u8')) {
      console.log('[RESPONSE M3U8]', response.status(), url.substring(0, 200));
      try {
        const body = await response.text();
        console.log('[M3U8 CONTENT]:', body.substring(0, 500));
      } catch(e) {}
      foundUrls.push({ type: 'm3u8', url });
    }
    if (url.includes('.mp4') || contentType.includes('video')) {
      console.log('[RESPONSE VIDEO]', response.status(), url.substring(0, 200));
      foundUrls.push({ type: 'video', url });
    }
  });
  
  console.log('Navigating to', embedUrl);
  try {
    await page.goto(embedUrl, { waitUntil: 'networkidle2', timeout: 30000 });
  } catch(e) {
    console.log('Navigation timeout (expected):', e.message.substring(0, 100));
  }
  
  // Wait extra for any lazy-loaded content
  console.log('Waiting for video element or m3u8 requests...');
  await new Promise(r => setTimeout(r, 10000));
  
  // Try to find video element in the page
  const videoSrc = await page.evaluate(() => {
    const video = document.querySelector('video');
    if (video) {
      return {
        src: video.src || video.currentSrc,
        sources: Array.from(video.querySelectorAll('source')).map(s => ({ src: s.src, type: s.type }))
      };
    }
    // Check for jwplayer or other players
    if (typeof jwplayer !== 'undefined') {
      try { return { jwplayer: jwplayer().getPlaylistItem() }; } catch(e) {}
    }
    // Check for Plyr
    if (typeof player !== 'undefined') {
      try { return { player: player.source }; } catch(e) {}
    }
    return null;
  }).catch(() => null);
  
  if (videoSrc) {
    console.log('\n=== Video Element Found ===');
    console.log(JSON.stringify(videoSrc, null, 2));
  }
  
  // Summary
  console.log('\n=== Found URLs Summary ===');
  if (foundUrls.length === 0) {
    console.log('No video URLs intercepted.');
    
    // Dump page content for inspection
    const content = await page.content();
    console.log('\n=== Page HTML (first 5000 chars) ===');
    console.log(content.substring(0, 5000));
  } else {
    foundUrls.forEach(u => console.log(`[${u.type}] ${u.url}`));
    
    // If m3u8 found, print it clearly
    const m3u8 = foundUrls.find(u => u.type === 'm3u8' || u.url.includes('.m3u8'));
    if (m3u8) {
      console.log('\n\n========================================');
      console.log('M3U8 URL EXTRACTED:');
      console.log(m3u8.url);
      console.log('========================================');
    }
  }
  
  await browser.close();
  return foundUrls;
}

extractM3U8('https://hgcloud.to/e/pcwv2guza7gi').catch(console.error);
