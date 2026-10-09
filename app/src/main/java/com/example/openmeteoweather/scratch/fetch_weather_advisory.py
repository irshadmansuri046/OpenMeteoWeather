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
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"Error posting {url}: {e}")
        return None

payload = {
    "latitude": 19.07,
    "longitude": 72.88,
    "crop_stages": [
        {
            "crop_id": 1, # Sugarcane
            "stage_id": 1 # Sowing/Germination
        }
    ]
}

res = fetch_post("advisory", payload)
print("Response from /weather/advisory:")
print(json.dumps(res, indent=2))
