const mongoose = require('mongoose');
mongoose.connect('mongodb+srv://vishu09921202023_db_user:GYeX5ttXbU6yaFMM@cluster0.6rhywrd.mongodb.net/cinevault')
  .then(async () => {
    const r = await mongoose.connection.collection('appversions').updateOne(
      {},
      { $set: { versionCode: 54, versionName: '2.1.6', releaseNotes: 'v2.1.6: Comprehensive admin delete user - cleans all related data' } }
    );
    console.log('Updated:', JSON.stringify(r));
    process.exit(0);
  })
  .catch(e => { console.error(e.message); process.exit(1); });
