import urllib.request, json, os
t = os.environ['GTOKEN']
req = urllib.request.Request(
    'https://run.googleapis.com/v2/projects/cinevault-streaming/locations/asia-south1/services/velora-backend',
    headers={'Authorization': f'Bearer {t}'}
)
resp = urllib.request.urlopen(req, timeout=15)
data = json.loads(resp.read().decode())
envs = data.get('template', {}).get('containers', [{}])[0].get('env', [])
for e in envs:
    n = e.get('name', '').upper()
    if 'GOOGLE' in n or 'CLIENT' in n or 'SECRET' in n or 'OAUTH' in n:
        print(f"{e['name']}={e.get('value', '[set]')}")
if not any('GOOGLE' in e.get('name','').upper() or 'CLIENT' in e.get('name','').upper() for e in envs):
    print("No Google/Client env vars found")
    print("All env var names:", [e['name'] for e in envs])
