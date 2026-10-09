import json

dest = "/Users/irshadmansuri/Documents/Antigravity/OpenMeteoWeather/app/src/main/java/com/example/openmeteoweather/scratch/openapi.json"

with open(dest, "r") as f:
    schema = json.load(f)

schemas = schema.get("components", {}).get("schemas", {})

print("Advisories property detail in WeatherCustomResponse:")
custom_res_schema = schemas.get("WeatherCustomResponse", {})
advisories_prop = custom_res_schema.get("properties", {}).get("advisories", {})
print("Type:", advisories_prop.get("type"))
print("Items:", advisories_prop.get("items"))

# Print details of any schemas that contain 'Advisory'
print("\nSchemas containing 'Advisory':")
for name, val in schemas.items():
    if "advisory" in name.lower():
        print(f"Schema: {name}")
        print(json.dumps(val, indent=2))

print("\nDetail of /weather/advisory endpoint:")
advisory_path = schema.get("paths", {}).get("/weather/advisory", {})
print(json.dumps(advisory_path, indent=2))
