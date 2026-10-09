import urllib.request

url = "https://swanweather-production.up.railway.app/openapi.json"
dest = "/Users/irshadmansuri/Documents/Antigravity/OpenMeteoWeather/app/src/main/java/com/example/openmeteoweather/scratch/openapi.json"

try:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        data = response.read().decode('utf-8')
        with open(dest, "w") as f:
            f.write(data)
        print("Successfully saved OpenAPI to openapi.json")
except Exception as e:
    print("Failed:", e)
