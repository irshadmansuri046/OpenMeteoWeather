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
    print(f"Total crops: {len(crops)}. Checking first 15 crops...")
    found = False
    for idx, c in enumerate(crops[:15]):
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
            # Try /weather/advisory
            res_advisory = fetch_post("advisory", payload)
            if res_advisory:
                advs = res_advisory.get("advisories", [])
                if advs:
                    print(f"FOUND in /weather/advisory for Crop ID {crop_id} ({c['name']}), Stage ID {stage_id} ({s['stage_name']}):")
                    print(json.dumps(res_advisory, indent=2))
                    found = True
                    break
            
            # Try /weather/custom
            res_custom = fetch_post("custom", payload)
            if res_custom:
                advs = res_custom.get("advisories", [])
                if advs:
                    print(f"FOUND in /weather/custom for Crop ID {crop_id} ({c['name']}), Stage ID {stage_id} ({s['stage_name']}):")
                    print(json.dumps(res_custom, indent=2))
                    found = True
                    break
        if found:
            break
    if not found:
        print("Checked first 15 crops and found no advisories.")
