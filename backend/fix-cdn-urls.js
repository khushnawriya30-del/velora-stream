/**
 * One-time script: Remove bad Bunny CDN URLs from movies and episodes.
 * All CDN files are 2KB HTML "Quota exceeded" pages, not actual video.
 * This script removes those bad streaming sources and deletes files from Bunny Storage.
 */
const { MongoClient } = require('mongodb');

const MONGO_URI = 'mongodb+srv://vishu09921202023_db_user:GYeX5ttXbU6yaFMM@cluster0.6rhywrd.mongodb.net/cinevault?appName=Cluster0';
const BUNNY_STORAGE_HOST = 'sg.storage.bunnycdn.com';
const BUNNY_ZONE = 'cinevault-videos';
const BUNNY_KEY = '6de9d73d-8d36-43f7-869d393f1d6a-9285-48ab';
const CDN_URL = 'https://cinevault-cdn.b-cdn.net';

function isBunnyUrl(url) {
  return url && (url.includes('b-cdn.net') || url.includes('bunnycdn.com'));
}

async function deleteBunnyFile(cdnUrl) {
  try {
    const path = cdnUrl.replace(CDN_URL + '/', '');
    const deleteUrl = `https://${BUNNY_STORAGE_HOST}/${BUNNY_ZONE}/${path}`;
    const res = await fetch(deleteUrl, {
      method: 'DELETE',
      headers: { AccessKey: BUNNY_KEY },
    });
    return res.ok;
  } catch {
    return false;
  }
}

async function main() {
  const client = new MongoClient(MONGO_URI);
  await client.connect();
  const db = client.db('cinevault');

  // --- Fix Movies ---
  const movies = await db.collection('movies').find({}).toArray();
  let moviesFixed = 0;
  let bunnyDeleted = 0;

  for (const movie of movies) {
    let changed = false;

    if (movie.streamingSources?.length) {
      const validSources = [];
      for (const src of movie.streamingSources) {
        if (isBunnyUrl(src.url)) {
          console.log(`  Removing bad CDN source from movie "${movie.title}": ${src.url}`);
          const deleted = await deleteBunnyFile(src.url);
          if (deleted) bunnyDeleted++;
          changed = true;
          // Don't keep this source
        } else {
          validSources.push(src);
        }
      }
      if (changed) {
        await db.collection('movies').updateOne(
          { _id: movie._id },
          { $set: { streamingSources: validSources } }
        );
        moviesFixed++;
      }
    }

    if (movie.trailerUrl && isBunnyUrl(movie.trailerUrl)) {
      console.log(`  Removing bad CDN trailer from movie "${movie.title}": ${movie.trailerUrl}`);
      const deleted = await deleteBunnyFile(movie.trailerUrl);
      if (deleted) bunnyDeleted++;
      await db.collection('movies').updateOne(
        { _id: movie._id },
        { $set: { trailerUrl: '' } }
      );
      if (!changed) moviesFixed++;
    }
  }

  // --- Fix Episodes ---
  const episodes = await db.collection('episodes').find({}).toArray();
  let episodesFixed = 0;

  for (const ep of episodes) {
    if (ep.streamingSources?.length) {
      const validSources = [];
      let changed = false;
      for (const src of ep.streamingSources) {
        if (isBunnyUrl(src.url)) {
          console.log(`  Removing bad CDN source from episode "${ep.title || 'Ep' + ep.episodeNumber}": ${src.url}`);
          const deleted = await deleteBunnyFile(src.url);
          if (deleted) bunnyDeleted++;
          changed = true;
        } else {
          validSources.push(src);
        }
      }
      if (changed) {
        await db.collection('episodes').updateOne(
          { _id: ep._id },
          { $set: { streamingSources: validSources } }
        );
        episodesFixed++;
      }
    }
  }

  console.log('\n=== REVERT COMPLETE ===');
  console.log(`Movies fixed: ${moviesFixed}`);
  console.log(`Episodes fixed: ${episodesFixed}`);
  console.log(`Bunny files deleted: ${bunnyDeleted}`);

  await client.close();
}

main().catch(console.error);
