const mongoose = require('mongoose');
mongoose.connect('mongodb+srv://vishu09921202023_db_user:GYeX5ttXbU6yaFMM@cluster0.6rhywrd.mongodb.net/cinevault')
  .then(async () => {
    const r = await mongoose.connection.collection('appversions').updateOne(
      {},
      { $set: { versionCode: 72, versionName: '2.5.1', releaseNotes: 'v2.5.1: Premium badge on SectionDetail grid, Search results, full badge consistency' } }
    );
    console.log('Updated:', JSON.stringify(r));
    process.exit(0);
  })
  .catch(e => { console.error(e.message); process.exit(1); });
