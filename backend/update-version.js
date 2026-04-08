const mongoose = require('mongoose');
mongoose.connect('mongodb+srv://vishu09921202023_db_user:GYeX5ttXbU6yaFMM@cluster0.6rhywrd.mongodb.net/cinevault')
  .then(async () => {
    const r = await mongoose.connection.collection('appversions').updateOne(
      {},
      { $set: { versionCode: 36, versionName: '1.9.7', releaseNotes: 'v1.9.7: banner text to price gap 16dp' } }
    );
    console.log('Updated:', JSON.stringify(r));
    process.exit(0);
  })
  .catch(e => { console.error(e.message); process.exit(1); });
