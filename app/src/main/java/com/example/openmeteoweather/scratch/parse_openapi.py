import json

dest = "/Users/irshadmansuri/Documents/Antigravity/OpenMeteoWeather/app/src/main/java/com/example/openmeteoweather/scratch/openapi.json"

with open(dest, "r") as f:
    schema = json.load(f)

print("Paths in API:")
for path in schema.get("paths", {}).keys():
    print(f"  {path}")
    for method in schema["paths"][path].keys():
        print(f"    Method: {method}")

print("\nDetail of /weather/custom (or custom paths):")
custom_paths = [p for p in schema.get("paths", {}).keys() if "custom" in p]
for cp in custom_paths:
    print(f"Path: {cp}")
    for method in schema["paths"][cp].keys():
        m_info = schema["paths"][cp][method]
        print(f"  Method: {method}, Summary: {m_info.get('summary')}")
        if 'requestBody' in m_info:
            content = m_info['requestBody'].get('content', {})
            for ct in content.keys():
                print(f"    Request Content Type: {ct}")
                ref = content[ct].get('schema', {}).get('$ref')
                if ref:
                    print(f"      Ref: {ref}")
        if 'responses' in m_info:
            res_200 = m_info['responses'].get('200', {}).get('content', {}).get('application/json', {}).get('schema', {})
            ref = res_200.get('$ref')
            if ref:
                 print(f"    Response 200 Ref: {ref}")

print("\nComponents Schemas:")
schemas = schema.get("components", {}).get("schemas", {})
for s_name in schemas.keys():
    if "custom" in s_name.lower() or "advisory" in s_name.lower() or "crop" in s_name.lower():
        print(f"Schema Name: {s_name}")
        props = schemas[s_name].get("properties", {})
        print(f"  Properties: {list(props.keys())}")
        for p_name, p_val in props.items():
            print(f"    {p_name}: {p_val.get('type')} / Ref: {p_val.get('$ref')} / Items: {p_val.get('items')}")
