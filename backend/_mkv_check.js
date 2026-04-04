// Check if MKV file has SeekHead -> Cues reference
// Matroska EBML IDs:
// SeekHead: 0x114D9B74
// Seek: 0x4DBB
// SeekID: 0x53AB
// SeekPosition: 0x53AC
// Cues ID: 0x1C53BB6B
// Segment Info ID: 0x1549A966
// Duration element: 0x4489

const https = require('https');

const url = 'https://pub-a28e4b1a86ad4529ab377c52314a9dec.r2.dev/Dhurandhar%20The%20Revenge%202026%20Hindi%20HDTC%201080p%2010bit%20x265%20HEVC%20HC-ESub-Vegamovies.is%20(1).mkv';

// Download first 64KB to analyze MKV header
const options = { headers: { Range: 'bytes=0-65535' } };

https.get(url, options, (res) => {
  const chunks = [];
  res.on('data', c => chunks.push(c));
  res.on('end', () => {
    const buf = Buffer.concat(chunks);
    console.log(`Downloaded ${buf.length} bytes for analysis`);
    console.log(`First 4 bytes (hex): ${buf.slice(0, 4).toString('hex')}`);
    
    // EBML header should start with 0x1A45DFA3
    if (buf[0] === 0x1A && buf[1] === 0x45 && buf[2] === 0xDF && buf[3] === 0xA3) {
      console.log('✅ Valid EBML/Matroska header');
    } else {
      console.log('❌ NOT a valid Matroska file!');
      return;
    }
    
    // Search for SeekHead element ID: 0x114D9B74
    const seekHeadId = Buffer.from([0x11, 0x4D, 0x9B, 0x74]);
    const seekHeadPos = buf.indexOf(seekHeadId);
    console.log(`SeekHead element at byte: ${seekHeadPos}`);
    
    // Search for Cues ID reference inside SeekHead: 0x1C53BB6B
    const cuesId = Buffer.from([0x1C, 0x53, 0xBB, 0x6B]);
    const cuesRefPos = buf.indexOf(cuesId);
    console.log(`Cues ID reference at byte: ${cuesRefPos}`);
    
    if (cuesRefPos > 0) {
      console.log('✅ SeekHead references Cues element — ExoPlayer should find and read it');
    } else {
      console.log('⚠️ No Cues reference found in first 64KB — Cues may be inline or missing');
    }
    
    // Search for Segment Info ID: 0x1549A966
    const infoId = Buffer.from([0x15, 0x49, 0xA9, 0x66]);
    const infoPos = buf.indexOf(infoId);
    console.log(`SegmentInfo element at byte: ${infoPos}`);
    
    // Search for Duration element: 0x4489
    const durId = Buffer.from([0x44, 0x89]);
    let durPos = -1;
    for (let i = 0; i < buf.length - 1; i++) {
      if (buf[i] === 0x44 && buf[i+1] === 0x89) {
        durPos = i;
        break;
      }
    }
    console.log(`Duration element at byte: ${durPos}`);
    if (durPos > 0) {
      console.log('✅ Duration present in header');
    }
    
    // Search for Cluster ID: 0x1F43B675
    const clusterId = Buffer.from([0x1F, 0x43, 0xB6, 0x75]);
    const clusterPos = buf.indexOf(clusterId);
    console.log(`First Cluster at byte: ${clusterPos}`);
    
    // Search for Tracks ID: 0x1654AE6B
    const tracksId = Buffer.from([0x16, 0x54, 0xAE, 0x6B]);
    const tracksPos = buf.indexOf(tracksId);
    console.log(`Tracks element at byte: ${tracksPos}`);
    
    console.log('\n--- Summary ---');
    console.log(`File structure within first 64KB:`);
    const elements = [];
    if (seekHeadPos >= 0) elements.push({ name: 'SeekHead', pos: seekHeadPos });
    if (infoPos >= 0) elements.push({ name: 'SegmentInfo', pos: infoPos });
    if (tracksPos >= 0) elements.push({ name: 'Tracks', pos: tracksPos });
    if (cuesRefPos >= 0) elements.push({ name: 'CuesRef', pos: cuesRefPos });
    if (clusterPos >= 0) elements.push({ name: 'FirstCluster', pos: clusterPos });
    elements.sort((a, b) => a.pos - b.pos);
    elements.forEach(e => console.log(`  ${e.name}: byte ${e.pos}`));
  });
}).on('error', e => console.error(e));
