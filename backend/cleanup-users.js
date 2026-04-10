const { MongoClient, ObjectId } = require('mongodb');

const uri = 'mongodb+srv://vishu09921202023_db_user:GYeX5ttXbU6yaFMM@cluster0.6rhywrd.mongodb.net/cinevault';

async function main() {
  const client = new MongoClient(uri);
  await client.connect();
  const db = client.db('cinevault');

  // Find user to keep
  const keeper = await db.collection('users').findOne({ email: 'singhshegal007@gmail.com' });
  console.log('Keeping user:', keeper?.email, keeper?._id);

  // Find all users to delete
  const toDelete = await db.collection('users').find({ email: { $ne: 'singhshegal007@gmail.com' } }).toArray();
  console.log('Users to delete:', toDelete.length);
  toDelete.forEach(u => console.log('  -', u.email, u._id.toString()));

  const ids = toDelete.map(u => u._id);

  // Delete from all collections
  const r1 = await db.collection('watchprogresses').deleteMany({ userId: { $in: ids } });
  console.log('watchProgress deleted:', r1.deletedCount);
  const r2 = await db.collection('wallets').deleteMany({ userId: { $in: ids } });
  console.log('wallets deleted:', r2.deletedCount);
  const r3 = await db.collection('referrals').deleteMany({ $or: [{ referrerId: { $in: ids } }, { newUserId: { $in: ids } }] });
  console.log('referrals deleted:', r3.deletedCount);
  const r4 = await db.collection('profiles').deleteMany({ userId: { $in: ids } });
  console.log('profiles deleted:', r4.deletedCount);
  const r5 = await db.collection('watchlists').deleteMany({ userId: { $in: ids } });
  console.log('watchlists deleted:', r5.deletedCount);
  const r6 = await db.collection('reviews').deleteMany({ userId: { $in: ids } });
  console.log('reviews deleted:', r6.deletedCount);
  const r7 = await db.collection('withdrawals').deleteMany({ userId: { $in: ids } });
  console.log('withdrawals deleted:', r7.deletedCount);
  const r8 = await db.collection('contentviews').deleteMany({ userId: { $in: ids } });
  console.log('contentViews deleted:', r8.deletedCount);
  const r9 = await db.collection('pendingreferralvisits').deleteMany({});
  console.log('pendingReferralVisits deleted:', r9.deletedCount);
  const r10 = await db.collection('users').deleteMany({ email: { $ne: 'singhshegal007@gmail.com' } });
  console.log('users deleted:', r10.deletedCount);

  // Reset the kept user so they can be referred again
  await db.collection('users').updateOne(
    { email: 'singhshegal007@gmail.com' },
    { $unset: { referredBy: '' } }
  );

  // Reset wallet to clean state
  await db.collection('wallets').updateOne(
    { userId: keeper._id },
    { $set: { balance: 0, totalEarned: 0, totalWithdrawn: 0, totalReferrals: 0 } }
  );

  // Verification
  console.log('\n--- Verification ---');
  console.log('Remaining users:', await db.collection('users').countDocuments());
  console.log('Remaining wallets:', await db.collection('wallets').countDocuments());
  console.log('Remaining referrals:', await db.collection('referrals').countDocuments());
  const keptUser = await db.collection('users').findOne({ email: 'singhshegal007@gmail.com' });
  console.log('Kept user referredBy:', keptUser?.referredBy);
  const keptWallet = await db.collection('wallets').findOne({ userId: keeper._id });
  console.log('Kept wallet balance:', keptWallet?.balance);

  await client.close();
}

main().catch(console.error);
