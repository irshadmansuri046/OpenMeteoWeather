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

major_crops = [2, 1, 3, 124, 4] # Paddy, Sugarcane, Wheat, Cotton, Maize

for crop_id in major_crops:
    stages = fetch_get(f"crops/{crop_id}/stages")
    if not stages:
        continue
    print(f"Crop {crop_id}: {len(stages)} stages")
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
            if len(advisories) > 0:
                print(f"  Stage {stage_id} ({s['stage_name']}) returned {len(advisories)} advisories!")
                print(json.dumps(advisories, indent=2))
