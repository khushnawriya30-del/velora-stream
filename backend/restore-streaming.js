const { MongoClient } = require('mongodb');

async function main() {
  const c = new MongoClient('mongodb+srv://vishu09921202023_db_user:GYeX5ttXbU6yaFMM@cluster0.6rhywrd.mongodb.net/cinevault');
  await c.connect();
  const db = c.db('cinevault');

  // Restore episode streaming URLs from thumbnail Drive IDs
  const eps = await db.collection('episodes').find({
    thumbnailUrl: { $regex: 'drive.google.com/thumbnail' }
  }).toArray();

  console.log('Total episodes with Drive thumbnails:', eps.length);
  let updated = 0;
  for (const ep of eps) {
    const match = ep.thumbnailUrl.match(/id=([^&]+)/);
    if (!match) continue;
    const fileId = match[1];
    const cdnUrl = 'https://cinevault-cdn.b-cdn.net/stream/' + fileId;
    await db.collection('episodes').updateOne(
      { _id: ep._id },
      { $set: { streamingSources: [{ quality: '1080p', url: cdnUrl, format: 'mp4' }] } }
    );
    updated++;
  }
  console.log('Updated episodes:', updated);

  // Check movies for Drive thumbnail URLs too
  const movies = await db.collection('movies').find({}).toArray();
  console.log('\n--- Movies ---');
  for (const m of movies) {
    const sources = m.streamingSources || [];
    console.log(`${m.title}: ${sources.length} sources, thumbnailUrl: ${m.thumbnailUrl || 'none'}`);
  }

  await c.close();
}

main().catch(console.error);
