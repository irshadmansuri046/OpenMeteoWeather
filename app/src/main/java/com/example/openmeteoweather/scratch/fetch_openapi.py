import urllib.request
import json

base_urls = [
    "https://swanweather-production.up.railway.app/openapi.json",
    "https://swanweather-production.up.railway.app/weather/openapi.json",
    "https://swanweather-production.up.railway.app/",
    "https://swanweather-production.up.railway.app/weather/"
]

for url in base_urls:
    print(f"Trying: {url}")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            data = response.read().decode('utf-8')
            # Try parsing JSON
            try:
                js = json.loads(data)
                print(f"SUCCESS JSON from {url}!")
                print(json.dumps(js, indent=2)[:2000]) # Print first 2000 chars of OpenAPI json
            except:
                print(f"SUCCESS TEXT/HTML from {url}!")
                print(data[:500])
    except Exception as e:
        print(f"Failed: {e}")
