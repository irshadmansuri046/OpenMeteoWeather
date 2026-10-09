import urllib.request
import json

base_url = "https://swanweather-production.up.railway.app/weather/"

def fetch_get(path):
    url = base_url + path
    try:
        with urllib.request.urlopen(url) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        return None

def fetch_post(path, payload):
    url = base_url + path
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='POST'
    )
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        return None

crops = fetch_get("crops")
if crops:
    print(f"Total crops: {len(crops)}. Checking first 30 crops...")
    for idx, c in enumerate(crops[:30]):
        crop_id = c['id']
        stages = fetch_get(f"crops/{crop_id}/stages")
        if not stages:
            continue
        for s in stages:
            stage_id = s['id']
            payload = {
                "latitude": 19.07,
                "longitude": 72.88,
                "crop_stages": [
                    {
                        "crop_id": crop_id,
                        "stage_id": stage_id
                    }
                ]
            }
            res = fetch_post("custom", payload)
            if res:
                advisories = res.get("advisories", [])
                # Also check inside 'data' or anywhere else
                if advisories or 'advisory' in str(res).lower() or 'alert' in str(res).lower():
                    # Check if it has non-empty advisories
                    if advisories:
                        print(f"FOUND! Crop ID {crop_id} ({c['name']}), Stage ID {stage_id} ({s['stage_name']}) returns advisories:")
                        print(json.dumps(res, indent=2))
                        exit(0)
    print("Checked 30 crops and found no advisories.")
