import urllib.request
import json
from concurrent.futures import ThreadPoolExecutor, as_completed

base_url = "https://swanweather-production.up.railway.app/weather/"

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

# Generate combinations of crop_id and stage_id that are most common
# Crop 1 (Sugarcane): stages 1, 2, 3, 4
# Crop 2 (Paddy): stages 5, 6, 7, 8
# Crop 3 (Wheat): stages 9, 10, 11, 12
# Crop 4 (Maize): stages 13, 14, 15, 16, 17, 18, 19
# Crop 124 (Cotton): stages 479, 480, 481, 482, 483, 484
# Crop 112 (Aloe Vera): stages 431, 432, 433, 434
queries = []
for s in [1, 2, 3, 4]: queries.append((1, s))
for s in [5, 6, 7, 8]: queries.append((2, s))
for s in [9, 10, 11, 12]: queries.append((3, s))
for s in [13, 14, 15, 16, 17, 18, 19]: queries.append((4, s))
for s in [431, 432, 433, 434]: queries.append((112, s))
for s in [479, 480, 481, 482, 483, 484]: queries.append((124, s))

print(f"Total queries to run in parallel: {len(queries)}")

def check_one(crop_id, stage_id):
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
    res_advisory = fetch_post("advisory", payload)
    adv_advisory = res_advisory.get("advisories", []) if res_advisory else []
    
    res_custom = fetch_post("custom", payload)
    adv_custom = res_custom.get("advisories", []) if res_custom else []
    
    if adv_advisory or adv_custom:
        return {
            "crop_id": crop_id,
            "stage_id": stage_id,
            "advisory": adv_advisory,
            "custom": adv_custom
        }
    return None

results = []
with ThreadPoolExecutor(max_workers=20) as executor:
    futures = {executor.submit(check_one, cid, sid): (cid, sid) for cid, sid in queries}
    for future in as_completed(futures):
        res = future.result()
        if res:
            results.append(res)
            print(f"Found advisory for crop {res['crop_id']} stage {res['stage_id']}!")

with open("/Users/irshadmansuri/Documents/Antigravity/OpenMeteoWeather/app/src/main/java/com/example/openmeteoweather/scratch/result_fast.json", "w") as f:
    json.dump(results if results else {"message": "No advisories found"}, f, indent=2)

print("Finished parallel checks.")
