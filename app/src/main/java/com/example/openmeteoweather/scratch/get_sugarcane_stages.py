import urllib.request
import json

base_url = "https://swanweather-production.up.railway.app/weather/"

def fetch_get(path):
    url = base_url + path
    try:
        with urllib.request.urlopen(url) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return None

print("STAGES FOR SUGARCANE (ID 1):")
print(json.dumps(fetch_get("crops/1/stages"), indent=2))

print("\nSTAGES FOR PADDY (ID 2):")
print(json.dumps(fetch_get("crops/2/stages"), indent=2))
