import urllib.request
import json
from concurrent.futures import ThreadPoolExecutor, as_completed

base_url = "https://swanweather-production.up.railway.app/weather/"

def fetch_get(path):
    url = base_url + path
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=5) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        return None

def fetch_post(path, payload):
    url = base_url + path
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json', 'User-Agent': 'Mozilla/5.0'},
        method='POST'
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        return None

try:
    crops = fetch_get("crops")
    all_queries = []
    # Check first 30 crops
    for c in crops[:30]:
        crop_id = c['id']
        stages = fetch_get(f"crops/{crop_id}/stages")
        if stages:
            for s in stages:
                all_queries.append((c, s))

    results = []

    def check_one(crop, stage):
        payload = {
            "latitude": 19.07,
            "longitude": 72.88,
            "crop_stages": [
                {
                    "crop_id": crop['id'],
                    "stage_id": stage['id']
                }
            ]
        }
        res_advisory = fetch_post("advisory", payload)
        adv_advisory = res_advisory.get("advisories", []) if res_advisory else []
        
        res_custom = fetch_post("custom", payload)
        adv_custom = res_custom.get("advisories", []) if res_custom else []
        
        if adv_advisory or adv_custom:
            return {
                "crop": crop,
                "stage": stage,
                "advisory": adv_advisory,
                "custom": adv_custom
            }
        return None

    with ThreadPoolExecutor(max_workers=5) as executor:
        futures = {executor.submit(check_one, c, s): (c, s) for c, s in all_queries}
        for future in as_completed(futures):
            res = future.result()
            if res:
                results.append(res)
                # Write immediately
                with open("/Users/irshadmansuri/Documents/Antigravity/OpenMeteoWeather/app/src/main/java/com/example/openmeteoweather/scratch/result.json", "w") as f:
                    json.dump(results, f, indent=2)

    with open("/Users/irshadmansuri/Documents/Antigravity/OpenMeteoWeather/app/src/main/java/com/example/openmeteoweather/scratch/result.json", "w") as f:
        json.dump(results if results else {"message": "No advisories found"}, f, indent=2)
    print("Done checking. Saved to result.json")

except Exception as e:
    with open("/Users/irshadmansuri/Documents/Antigravity/OpenMeteoWeather/app/src/main/java/com/example/openmeteoweather/scratch/result.json", "w") as f:
        json.dump({"error": str(e)}, f, indent=2)
