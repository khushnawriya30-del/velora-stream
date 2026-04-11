const mongoose = require('mongoose');
mongoose.connect('mongodb+srv://vishu09921202023_db_user:GYeX5ttXbU6yaFMM@cluster0.6rhywrd.mongodb.net/cinevault')
  .then(async () => {
    const r = await mongoose.connection.collection('appversions').updateOne(
      {},
      { $set: { versionCode: 75, versionName: '2.5.4', releaseNotes: 'v2.5.4: Enhanced Premium Exclusive add modal with content type filter, auto-premium notice' } }
    );
    console.log('Updated:', JSON.stringify(r));
    process.exit(0);
  })
  .catch(e => { console.error(e.message); process.exit(1); });
