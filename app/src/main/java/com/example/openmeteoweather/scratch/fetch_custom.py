import urllib.request
import json

base_url = "https://swanweather-production.up.railway.app/weather/"

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
            data = response.read().decode('utf-8')
            return json.loads(data)
    except Exception as e:
        print(f"Error posting {url}: {e}")
        return None

custom_payload = {
    "latitude": 19.07,
    "longitude": 72.88,
    "crop_stages": [
        {
            "crop_id": 112,
            "stage_id": 431
        }
    ]
}
res = fetch_post("custom", custom_payload)
if res:
    print("STATUS:", res.get("status"))
    print("MESSAGE:", res.get("message"))
    advisories = res.get("advisories", [])
    print(f"Advisories size: {len(advisories)}")
    print(json.dumps(advisories, indent=2))
