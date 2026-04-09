const jwt = require('jsonwebtoken');
const { MongoClient } = require('mongodb');

async function main() {
  const c = new MongoClient('mongodb+srv://vishu09921202023_db_user:GYeX5ttXbU6yaFMM@cluster0.6rhywrd.mongodb.net/cinevault');
  await c.connect();
  const db = c.db('cinevault');
  
  // Get the first user (the one likely logged in)
  const user = await db.collection('users').findOne({ name: /nawar|vishu|khush/i });
  console.log('User:', user.name, user._id.toString(), 'referralCode:', user.referralCode);
  
  // Generate a valid JWT - payload uses 'sub' not '_id'
  const token = jwt.sign(
    { sub: user._id.toString(), email: user.email, role: user.role || 'user' },
    'cinevault-dev-access-secret-2026',
    { expiresIn: '15m' }
  );
  
  // Test the referral stats endpoint
  const resp = await fetch('https://velora-backend-761880285456.asia-south1.run.app/api/v1/referral/stats', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  console.log('Status:', resp.status);
  const body = await resp.text();
  console.log('Body:', body);
  
  // Also test via the old URL the app uses
  const resp2 = await fetch('https://velora-backend-fopqpbthva-el.a.run.app/api/v1/referral/stats', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  console.log('Old URL Status:', resp2.status);
  const body2 = await resp2.text();
  console.log('Old URL Body:', body2);
  
  await c.close();
}
main().catch(console.error);
