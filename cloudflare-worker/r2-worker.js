/**
 * CineVault R2 Media Worker — Cloudflare Worker with R2 Binding
 *
 * Serves video content from Cloudflare R2 with:
 *   - Range request support (seeking in video player)
 *   - CORS headers (cross-origin streaming)
 *   - Content-Type detection
 *   - Cache-Control optimization
 *   - Folder listing for admin browsing
 *
 * DEPLOYMENT:
 *   1. cd cloudflare-worker
 *   2. npx wrangler deploy -c wrangler-r2.toml
 *   3. Worker URL: https://r2-media.<your-subdomain>.workers.dev
 *   4. Set R2_PUBLIC_URL in backend to that URL
 *
 * R2 BUCKET BINDING:
 *   The R2 bucket is bound as "MEDIA" in wrangler-r2.toml
 */

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, HEAD, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Range, X-Api-Key',
  'Access-Control-Expose-Headers': 'Content-Length, Content-Range, Accept-Ranges, Content-Type',
};

const MIME_TYPES = {
  mp4: 'video/mp4',
  mkv: 'video/x-matroska',
  webm: 'video/webm',
  avi: 'video/x-msvideo',
  mov: 'video/quicktime',
  ts: 'video/mp2t',
  m3u8: 'application/vnd.apple.mpegurl',
  flv: 'video/x-flv',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
  srt: 'text/plain',
  vtt: 'text/vtt',
};

export default {
  async fetch(request, env) {
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    const url = new URL(request.url);

    try {
      // ── Health check ──
      if (url.pathname === '/' || url.pathname === '/health') {
        return jsonResponse({ status: 'ok', service: 'CineVault R2 Media Worker', bucket: 'velora-media' });
      }

      // ── List folder contents (for admin browsing) ──
      // GET /list?prefix=series/breaking-bad/s01/
      if (url.pathname === '/list') {
        return handleList(url, env);
      }

      // ── Multipart Upload: Create ──
      // POST /upload/multipart/create?key=series/show/s01/e01.mp4
      if (url.pathname === '/upload/multipart/create' && request.method === 'POST') {
        if (!checkApiKey(request, env)) return jsonResponse({ error: 'Unauthorized' }, 401);
        const key = url.searchParams.get('key');
        if (!key) return jsonResponse({ error: 'Missing ?key= parameter' }, 400);
        const upload = await env.MEDIA.createMultipartUpload(key);
        return jsonResponse({ uploadId: upload.uploadId, key });
      }

      // ── Multipart Upload: Upload Part ──
      // PUT /upload/multipart/part?key=...&uploadId=...&partNumber=1
      if (url.pathname === '/upload/multipart/part' && request.method === 'PUT') {
        if (!checkApiKey(request, env)) return jsonResponse({ error: 'Unauthorized' }, 401);
        const key = url.searchParams.get('key');
        const uploadId = url.searchParams.get('uploadId');
        const partNumber = parseInt(url.searchParams.get('partNumber') || '0');
        if (!key || !uploadId || !partNumber) return jsonResponse({ error: 'Missing key, uploadId, or partNumber' }, 400);
        const upload = env.MEDIA.resumeMultipartUpload(key, uploadId);
        const part = await upload.uploadPart(partNumber, request.body);
        return jsonResponse({ partNumber: part.partNumber, etag: part.etag });
      }

      // ── Multipart Upload: Complete ──
      // POST /upload/multipart/complete?key=...&uploadId=...
      if (url.pathname === '/upload/multipart/complete' && request.method === 'POST') {
        if (!checkApiKey(request, env)) return jsonResponse({ error: 'Unauthorized' }, 401);
        const key = url.searchParams.get('key');
        const uploadId = url.searchParams.get('uploadId');
        if (!key || !uploadId) return jsonResponse({ error: 'Missing key or uploadId' }, 400);
        const { parts } = await request.json();
        const upload = env.MEDIA.resumeMultipartUpload(key, uploadId);
        await upload.complete(parts);
        const publicUrl = `${url.origin}/${key}`;
        return jsonResponse({ success: true, key, publicUrl });
      }

      // ── Multipart Upload: Abort ──
      // DELETE /upload/multipart/abort?key=...&uploadId=...
      if (url.pathname === '/upload/multipart/abort' && request.method === 'DELETE') {
        if (!checkApiKey(request, env)) return jsonResponse({ error: 'Unauthorized' }, 401);
        const key = url.searchParams.get('key');
        const uploadId = url.searchParams.get('uploadId');
        if (!key || !uploadId) return jsonResponse({ error: 'Missing key or uploadId' }, 400);
        const upload = env.MEDIA.resumeMultipartUpload(key, uploadId);
        await upload.abort();
        return jsonResponse({ success: true });
      }

      // ── Upload file (PUT /upload/series/show/s01/e01.mp4) ──
      if (url.pathname.startsWith('/upload/') && request.method === 'PUT') {
        if (!checkApiKey(request, env)) return jsonResponse({ error: 'Unauthorized' }, 401);
        const key = decodeURIComponent(url.pathname.slice('/upload/'.length));
        return handleUpload(request, key, env);
      }

      // ── Create folder (PUT /folder/series/show/s01/) ──
      if (url.pathname.startsWith('/folder/') && request.method === 'PUT') {
        if (!checkApiKey(request, env)) return jsonResponse({ error: 'Unauthorized' }, 401);
        const key = decodeURIComponent(url.pathname.slice('/folder/'.length));
        return handleCreateFolder(key, env);
      }

      // ── Delete file (DELETE /delete/series/show/s01/e01.mp4) ──
      if (url.pathname.startsWith('/delete/') && request.method === 'DELETE') {
        if (!checkApiKey(request, env)) return jsonResponse({ error: 'Unauthorized' }, 401);
        const key = decodeURIComponent(url.pathname.slice('/delete/'.length));
        return handleDelete(key, env);
      }

      // ── Presigned info (GET /presign?key=series/show/s01/e01.mp4) ──
      // Returns the direct upload URL for the worker
      if (url.pathname === '/presign') {
        if (!checkApiKey(request, env)) return jsonResponse({ error: 'Unauthorized' }, 401);
        const key = url.searchParams.get('key');
        if (!key) return jsonResponse({ error: 'Missing ?key= parameter' }, 400);
        const uploadUrl = `${url.origin}/upload/${encodeURIComponent(key)}`;
        const publicUrl = `${url.origin}/${key}`;
        return jsonResponse({ uploadUrl, key, publicUrl });
      }

      // ── Serve file from R2 (default) ──
      // GET /series/breaking-bad/s01/e01-pilot.mp4
      const key = decodeURIComponent(url.pathname.slice(1)); // remove leading /
      if (!key) {
        return jsonResponse({ error: 'No file path specified' }, 400);
      }

      return handleGet(request, key, env);
    } catch (err) {
      return jsonResponse({ error: err.message || 'Internal error' }, 500);
    }
  },
};

// ── Serve a file from R2 with Range support ──
async function handleGet(request, key, env) {
  const ext = key.split('.').pop()?.toLowerCase() || '';

  // For MKV files, apply on-the-fly SeekHead fix so ExoPlayer can seek
  if (ext === 'mkv') {
    return handleMkvGet(request, key, env);
  }

  return handleGetPlain(request, key, env);
}

// ── Plain file serving (non-MKV or MKV fallback) ──
async function handleGetPlain(request, key, env) {
  const rangeHeader = request.headers.get('Range');
  const object = await env.MEDIA.get(key, {
    range: rangeHeader ? request.headers : undefined,
    onlyIf: request.headers,
  });

  if (!object) {
    return jsonResponse({ error: 'Not found', key }, 404);
  }

  const ext = key.split('.').pop()?.toLowerCase() || '';
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  const headers = new Headers({
    ...CORS_HEADERS,
    'Content-Type': contentType,
    'Accept-Ranges': 'bytes',
    'Cache-Control': 'public, max-age=86400, s-maxage=604800',
    'ETag': object.httpEtag,
  });

  if (object.range) {
    const { offset, length } = object.range;
    const end = offset + length - 1;
    headers.set('Content-Range', `bytes ${offset}-${end}/${object.size}`);
    headers.set('Content-Length', String(length));
    return new Response(object.body, { status: 206, headers });
  }

  headers.set('Content-Length', String(object.size));
  return new Response(object.body, { status: 200, headers });
}

// ═══════════════════════════════════════════════════════════════════════════════
// MKV SeekHead Fix — On-the-fly patching for video player seekability
//
// Many MKV muxers use a "metaseek" pattern: the first SeekHead near the start
// of the file only references a SECOND SeekHead at the end. That second SeekHead
// contains the actual Cues (seek-index) reference. ExoPlayer's MatroskaExtractor
// doesn't follow SeekHead → SeekHead chains, so it never finds the Cues and
// reports seekable=false. Seeking then snaps back to 0.
//
// Fix: detect this pattern, read the Cues position from the second SeekHead,
// and inject a new SeekHead element with a direct Cues reference into the Void
// padding that typically follows the first SeekHead. Total file size is unchanged.
// ═══════════════════════════════════════════════════════════════════════════════

const SEEK_HEAD_ID = [0x11, 0x4D, 0x9B, 0x74];
const CUES_ID      = [0x1C, 0x53, 0xBB, 0x6B];
const SEEK_EL_ID   = [0x4D, 0xBB];
const SEEKID_EL_ID = [0x53, 0xAB];
const SEEKPOS_EL_ID= [0x53, 0xAC];
const VOID_ID      = [0xEC];
const EBML_ID      = [0x1A, 0x45, 0xDF, 0xA3];
const SEGMENT_ID   = [0x18, 0x53, 0x80, 0x67];

/** Read an EBML variable-width integer (VINT) */
function readVint(data, offset) {
  if (offset >= data.length) return null;
  const first = data[offset];
  if (first === 0) return null;
  let width = 0;
  for (let mask = 0x80; mask > 0; mask >>= 1) {
    width++;
    if (first & mask) break;
  }
  if (width > 8 || offset + width > data.length) return null;
  let value = first & ((1 << (8 - width)) - 1);
  for (let i = 1; i < width; i++) value = value * 256 + data[offset + i];
  return { value, length: width };
}

/** Read an EBML element ID */
function readElementId(data, offset) {
  if (offset >= data.length) return null;
  const first = data[offset];
  let width;
  if (first >= 0x80) width = 1;
  else if (first >= 0x40) width = 2;
  else if (first >= 0x20) width = 3;
  else if (first >= 0x10) width = 4;
  else return null;
  if (offset + width > data.length) return null;
  return { id: Array.from(data.slice(offset, offset + width)), length: width };
}

function idEq(a, b) {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) if (a[i] !== b[i]) return false;
  return true;
}

/** Analyze MKV header to find SeekHead structure and Void padding */
function analyzeMkvHeader(data) {
  let pos = 0;
  // Skip EBML header
  const ebmlId = readElementId(data, pos);
  if (!ebmlId || !idEq(ebmlId.id, EBML_ID)) return null;
  pos += ebmlId.length;
  const ebmlSize = readVint(data, pos);
  if (!ebmlSize) return null;
  pos += ebmlSize.length + ebmlSize.value;

  // Read Segment header
  const segId = readElementId(data, pos);
  if (!segId || !idEq(segId.id, SEGMENT_ID)) return null;
  pos += segId.length;
  const segSize = readVint(data, pos);
  if (!segSize) return null;
  pos += segSize.length;
  const segmentDataStart = pos;

  // First element should be SeekHead
  const sh1Id = readElementId(data, pos);
  if (!sh1Id || !idEq(sh1Id.id, SEEK_HEAD_ID)) return null;
  pos += sh1Id.length;
  const sh1Size = readVint(data, pos);
  if (!sh1Size) return null;
  pos += sh1Size.length;
  const seekHeadEnd = pos + sh1Size.value;

  // Parse Seek entries
  let hasCuesRef = false;
  let secondSeekHeadPos = null;
  let innerPos = pos;
  while (innerPos < seekHeadEnd && innerPos < data.length) {
    const elId = readElementId(data, innerPos);
    if (!elId) break;
    innerPos += elId.length;
    const elSize = readVint(data, innerPos);
    if (!elSize) break;
    innerPos += elSize.length;
    if (idEq(elId.id, SEEK_EL_ID)) {
      let seekId = null, seekPosition = null;
      let sp = innerPos;
      const spEnd = innerPos + elSize.value;
      while (sp < spEnd && sp < data.length) {
        const sid = readElementId(data, sp);
        if (!sid) break;
        sp += sid.length;
        const ssz = readVint(data, sp);
        if (!ssz) break;
        sp += ssz.length;
        if (idEq(sid.id, SEEKID_EL_ID)) {
          seekId = Array.from(data.slice(sp, sp + ssz.value));
        } else if (idEq(sid.id, SEEKPOS_EL_ID)) {
          seekPosition = 0;
          for (let i = 0; i < ssz.value; i++) seekPosition = seekPosition * 256 + data[sp + i];
        }
        sp += ssz.value;
      }
      if (seekId && idEq(seekId, CUES_ID)) hasCuesRef = true;
      if (seekId && idEq(seekId, SEEK_HEAD_ID) && seekPosition !== null) secondSeekHeadPos = seekPosition;
    }
    innerPos += elSize.value;
  }

  // Check for Void padding after SeekHead
  let voidElement = null;
  if (seekHeadEnd < data.length) {
    const vId = readElementId(data, seekHeadEnd);
    if (vId && idEq(vId.id, VOID_ID)) {
      const vSz = readVint(data, seekHeadEnd + vId.length);
      if (vSz) {
        voidElement = {
          start: seekHeadEnd,
          end: seekHeadEnd + vId.length + vSz.length + vSz.value,
        };
      }
    }
  }

  return { segmentDataStart, hasCuesRef, secondSeekHeadPos, voidElement };
}

/** Find Cues SeekPosition inside a SeekHead element blob */
function findCuesPosition(data) {
  // Scan for SeekHead ID
  let pos = 0;
  while (pos < data.length - 4) {
    if (data[pos]===0x11 && data[pos+1]===0x4D && data[pos+2]===0x9B && data[pos+3]===0x74) break;
    pos++;
  }
  if (pos >= data.length - 4) return null;
  pos += 4;
  const shSize = readVint(data, pos);
  if (!shSize) return null;
  pos += shSize.length;
  const shEnd = pos + shSize.value;

  while (pos < shEnd && pos < data.length) {
    const elId = readElementId(data, pos);
    if (!elId) break;
    pos += elId.length;
    const elSize = readVint(data, pos);
    if (!elSize) break;
    pos += elSize.length;
    if (idEq(elId.id, SEEK_EL_ID)) {
      let seekId = null, seekPosition = null;
      let sp = pos;
      const spEnd = pos + elSize.value;
      while (sp < spEnd && sp < data.length) {
        const sid = readElementId(data, sp);
        if (!sid) break;
        sp += sid.length;
        const ssz = readVint(data, sp);
        if (!ssz) break;
        sp += ssz.length;
        if (idEq(sid.id, SEEKID_EL_ID)) seekId = Array.from(data.slice(sp, sp + ssz.value));
        else if (idEq(sid.id, SEEKPOS_EL_ID)) {
          seekPosition = 0;
          for (let i = 0; i < ssz.value; i++) seekPosition = seekPosition * 256 + data[sp + i];
        }
        sp += ssz.value;
      }
      if (seekId && idEq(seekId, CUES_ID) && seekPosition !== null) return seekPosition;
    }
    pos += elSize.value;
  }
  return null;
}

/** Build patch bytes: new SeekHead with Cues ref + remaining Void filler */
function buildCuesPatch(totalLen, cuesSeekPosition) {
  // SeekHead: ID(4) + size VINT(1) + Seek entry(17) = 22 bytes
  //   Seek: ID(2) + size(1) + SeekID elem(7) + SeekPosition elem(7) = 17
  const seekPositionBytes = [
    (cuesSeekPosition >>> 24) & 0xFF,
    (cuesSeekPosition >>> 16) & 0xFF,
    (cuesSeekPosition >>> 8)  & 0xFF,
     cuesSeekPosition         & 0xFF,
  ];
  const seekHead = new Uint8Array([
    0x11, 0x4D, 0x9B, 0x74,       // SeekHead ID
    0x80 | 17,                     // Size VINT = 17
    0x4D, 0xBB,                    // Seek ID
    0x80 | 14,                     // Seek size = 14
    0x53, 0xAB, 0x84,              // SeekID element (ID + size=4)
    0x1C, 0x53, 0xBB, 0x6B,       // Cues element ID
    0x53, 0xAC, 0x84,              // SeekPosition element (ID + size=4)
    ...seekPositionBytes,           // Cues position value
  ]);

  const remaining = totalLen - seekHead.length;
  if (remaining < 2) return null; // Not enough space for Void

  // Build Void filler
  const voidDataLen = remaining - 2; // 1 byte Void ID + 1 byte size VINT
  if (voidDataLen > 126) return null; // Would need multi-byte size, unlikely
  const patch = new Uint8Array(totalLen);
  patch.set(seekHead, 0);
  patch[seekHead.length] = 0xEC;              // Void ID
  patch[seekHead.length + 1] = 0x80 | voidDataLen; // Void size
  // Rest is already zeros (Void data)
  return patch;
}

/** Handle MKV file with optional SeekHead patching */
async function handleMkvGet(request, key, env) {
  const rangeHeader = request.headers.get('Range');
  let rangeStart = null;
  if (rangeHeader) {
    const m = rangeHeader.match(/bytes=(\d+)-/);
    if (m) rangeStart = parseInt(m[1]);
  }

  // Only patch if request covers byte 0
  if (rangeStart !== null && rangeStart > 0) {
    return handleGetPlain(request, key, env);
  }

  try {
    // Step 1: Read first 200 bytes to analyze SeekHead
    const headObj = await env.MEDIA.get(key, { range: { offset: 0, length: 200 } });
    if (!headObj) return handleGetPlain(request, key, env);
    const headData = new Uint8Array(await headObj.arrayBuffer());

    const analysis = analyzeMkvHeader(headData);
    if (!analysis || analysis.hasCuesRef || !analysis.secondSeekHeadPos || !analysis.voidElement) {
      return handleGetPlain(request, key, env); // No fix needed
    }

    // Step 2: Read second SeekHead from end of file
    const sh2AbsPos = analysis.segmentDataStart + analysis.secondSeekHeadPos;
    const tailObj = await env.MEDIA.get(key, { range: { offset: sh2AbsPos, length: 300 } });
    if (!tailObj) return handleGetPlain(request, key, env);
    const tailData = new Uint8Array(await tailObj.arrayBuffer());

    const cuesPos = findCuesPosition(tailData);
    if (cuesPos === null) return handleGetPlain(request, key, env);

    // Step 3: Build patch
    const voidLen = analysis.voidElement.end - analysis.voidElement.start;
    const patchBytes = buildCuesPatch(voidLen, cuesPos);
    if (!patchBytes) return handleGetPlain(request, key, env);

    // Step 4: Serve the patched response
    const object = await env.MEDIA.get(key, {
      range: rangeHeader ? request.headers : undefined,
      onlyIf: request.headers,
    });
    if (!object) return jsonResponse({ error: 'Not found', key }, 404);

    const patchStart = analysis.voidElement.start;
    const patchEnd = analysis.voidElement.end;
    const headers = new Headers({
      ...CORS_HEADERS,
      'Content-Type': 'video/x-matroska',
      'Accept-Ranges': 'bytes',
      'Cache-Control': 'public, max-age=86400, s-maxage=604800',
      'ETag': object.httpEtag,
    });

    let responseStart = 0;
    let status = 200;
    if (object.range) {
      responseStart = object.range.offset;
      const end = object.range.offset + object.range.length - 1;
      headers.set('Content-Range', `bytes ${responseStart}-${end}/${object.size}`);
      headers.set('Content-Length', String(object.range.length));
      status = 206;
    } else {
      headers.set('Content-Length', String(object.size));
    }

    // Patch the stream on the fly
    let bytesProcessed = 0;
    const { readable, writable } = new TransformStream({
      transform(chunk, controller) {
        const arr = new Uint8Array(chunk);
        const chunkStart = responseStart + bytesProcessed;
        const chunkEnd = chunkStart + arr.length;
        if (chunkEnd > patchStart && chunkStart < patchEnd) {
          const modified = new Uint8Array(arr);
          for (let i = 0; i < modified.length; i++) {
            const absPos = chunkStart + i;
            if (absPos >= patchStart && absPos < patchEnd) {
              modified[i] = patchBytes[absPos - patchStart];
            }
          }
          controller.enqueue(modified);
        } else {
          controller.enqueue(chunk);
        }
        bytesProcessed += arr.length;
      },
    });

    object.body.pipeTo(writable);
    return new Response(readable, { status, headers });
  } catch (e) {
    return handleGetPlain(request, key, env);
  }
}

// ── List folder contents ──
async function handleList(url, env) {
  const prefix = url.searchParams.get('prefix') || '';
  const delimiter = url.searchParams.get('delimiter') || '/';
  const limit = Math.min(parseInt(url.searchParams.get('limit') || '1000'), 1000);

  const listed = await env.MEDIA.list({
    prefix: prefix,
    delimiter: delimiter,
    limit: limit,
  });

  const folders = (listed.delimitedPrefixes || []).map((p) => ({
    name: p.replace(prefix, '').replace(/\/$/, ''),
    path: p,
  }));

  const files = (listed.objects || [])
    .filter((obj) => obj.key !== prefix)
    .map((obj) => ({
      name: obj.key.replace(prefix, ''),
      path: obj.key,
      size: obj.size,
      uploaded: obj.uploaded,
    }));

  return jsonResponse({
    prefix,
    folders,
    files,
    truncated: listed.truncated,
  });
}

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      ...CORS_HEADERS,
    },
  });
}

// ── Check API key for write operations ──
function checkApiKey(request, env) {
  const apiKey = env.API_KEY || 'velora-r2-default-key';
  const provided = request.headers.get('X-Api-Key') || new URL(request.url).searchParams.get('apiKey');
  return provided === apiKey;
}

// ── Upload a file to R2 ──
async function handleUpload(request, key, env) {
  const contentType = request.headers.get('Content-Type') || 'application/octet-stream';
  await env.MEDIA.put(key, request.body, {
    httpMetadata: { contentType },
  });
  const publicUrl = `${new URL(request.url).origin}/${key}`;
  return jsonResponse({ success: true, key, publicUrl, size: request.headers.get('Content-Length') });
}

// ── Create a folder marker ──
async function handleCreateFolder(key, env) {
  const folderKey = key.endsWith('/') ? key : key + '/';
  await env.MEDIA.put(folderKey, '', {
    httpMetadata: { contentType: 'application/x-directory' },
  });
  return jsonResponse({ success: true, path: folderKey });
}

// ── Delete a file from R2 ──
async function handleDelete(key, env) {
  await env.MEDIA.delete(key);
  return jsonResponse({ success: true, deleted: key });
}
