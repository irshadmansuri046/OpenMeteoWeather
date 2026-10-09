# Smart Weather Ensemble Engine & Crop Advisory: Logic Explainer
This document explains the intelligence behind the **Combined Weather** engine and the **Smart Agricultural Advisory** system. It describes how the system aggregates weather forecasts, remembers crop choices, and generates tailored advice for farmers.

---

## 1. The Core Analogy: The "Council of Weather Experts"
Imagine you are planning to spray or irrigate your fields, and you decide to consult four different weather experts:
1.  **Tomorrow.io** (The Lead Advisor)
2.  **Ambee** (The Local Specialist)
3.  **OpenWeather** (The Global Consultant)
4.  **Open-Meteo** (The Regional Analyst)

Instead of relying blindly on just one expert, you listen to all of them, weigh their opinions based on their historical accuracy, and check how well they agree with each other. This is exactly what the **Combined Weather Engine** does.

---

## 2. Determining the Consensus Forecast (Weighted Ensemble)
Each weather provider is assigned a "Trust Level" (weight) based on their general reliability:

| Weather Expert (Provider) | Trust Weight (Default) | Role in Calculations |
| :--- | :---: | :--- |
| **Tomorrow.io** | **35%** | Primary Lead (heaviest influence) |
| **Ambee** | **25%** | Strong Secondary Advisor |
| **OpenWeather** | **20%** | Supporting Global Context |
| **Open-Meteo** | **20%** | Supporting Regional Context |

### Dynamic Weight Redistribution (Handling Absent Experts)
If a provider fails to respond (due to a network timeout or API rate limit), the system doesn't break. It dynamically adjusts the weights of the active providers so that the total weight always equals **100%**.

> **Example**: If **Ambee (25%)** is offline, the remaining 75% of weights (Tomorrow.io: 35%, OpenWeather: 20%, Open-Meteo: 20%) are proportionally scaled up to:
> *   **Tomorrow.io**: ~46.7%
> *   **OpenWeather**: ~26.7%
> *   **Open-Meteo**: ~26.7%

### How Consensus Values are Calculated:
*   **Numbers (Temperature, Wind, Humidity, Rain Probability)**: The engine calculates a **weighted average**. 
    *   *Formula*: `(Temp1 * Weight1) + (Temp2 * Weight2) + ...`
    *   This ensures that the final temperature matches the collective intelligence of all online providers.
*   **Visuals & Icons (Weather Code, Sunrise, Sunset)**: Since you cannot take an average of "Sunny" and "Rainy" icons, the engine uses the visual state of the **highest-weighted successful provider** (usually Tomorrow.io) for the dashboard display.

---

## 3. Anomaly Detection & Anomaly Thresholds
Sometimes, one provider might give a wildly incorrect forecast (an anomaly or outlier) due to local sensor issues or mathematical glitches. The engine runs checks to detect these anomalies.

### Agreement Scaling & Thresholds
For every variable, the engine measures how far each provider deviates from the final weighted consensus. It compares these deviations against set **Scale Thresholds**:

| Weather Parameter | Max Deviation Scale Threshold | Explanation in Simple Terms |
| :--- | :---: | :--- |
| **Temperature** | **$8^\circ\text{C}$** | If an expert's temp forecast is off by $8^\circ\text{C}$ or more from the consensus, their temperature agreement score drops to **0%**. |
| **Wind Speed** | **20 km/h** | If an expert's wind speed differs by 20 km/h or more, their wind agreement score drops to **0%**. |
| **Rain Probability** | **80%** | If an expert's rain chance is off by 80% or more, their rain agreement score drops to **0%**. |
| **Humidity** | **80%** | If an expert's humidity differs by 80% or more, their humidity agreement score drops to **0%**. |

By calculating these individual agreement scores, the engine computes a final **Confidence Score** and **Confidence Level**:
*   **Very High Confidence** ($>85\%$ Agreement)
*   **High Confidence** ($70\% - 85\%$ Agreement)
*   **Medium Confidence** ($50\% - 70\%$ Agreement)
*   **Low Confidence** ($<50\%$ Agreement)

---

## 4. Smart Weather Alert Selection Logic
Once the consensus is calculated and anomalies are assessed, the engine selects specific alerts to warn the user about incoming weather patterns:

### 1. The "Forecast Disagreement" Alert (Anomaly Warning)
If the providers disagree strongly on whether it will rain tomorrow, the engine raises an anomaly alert.
*   **Trigger Threshold**: If the difference between the highest predicted rain chance and the lowest predicted rain chance among successful providers is **greater than 40%** (e.g., Tomorrow.io says 10% chance of rain, but OpenWeather says 60% chance).
*   **Action**: Triggers a `"Forecast Disagreement Detected"` warning, showing the spread and advising the farmer to monitor the situation closely before making heavy irrigation decisions.

### 2. High-Severity Consensus Rain Alert
*   **Trigger**: If the consensus rain probability is **greater than 60%**, and **all** active providers agree on this high probability.
*   **Action**: Triggers a strong alert: `"Consensus Rain Advisory: All providers indicate strong rainfall probability. Delay pesticide spraying and irrigation."`

---

## 5. Crop and Growth Stage Selection (How It Works)
To make the weather alerts practical, the app allows the farmer to choose which crop they are growing and what growth stage it is currently in.

### 1. Persistent Selection (Remembering Choices)
*   **No Selection, No Dashboard**: When the app is launched for the first time, it checks if a crop has been configured. If not, it automatically shows a selection sheet.
*   **Local Storage**: The selected crop (e.g., Orange) and stage (e.g., Flowering) are saved locally on the device. This ensures the app remembers these selections without needing internet access or account logins.
*   **Single Crop Focus**: To keep things clear and prevent conflicting instructions, the app only tracks **one active crop** selection at a time.

### 2. Crop Growth Stages
Each crop has different phases of life. For example:
*   **Orange**: Flowering $\rightarrow$ Fruit Set $\rightarrow$ Fruit Development $\rightarrow$ Harvest
*   **Potato**: Sprouting $\rightarrow$ Vegetative $\rightarrow$ Tuber Initiation $\rightarrow$ Tuber Bulking $\rightarrow$ Harvest
*   **Sesame**: Sowing $\rightarrow$ Seedling $\rightarrow$ Flowering $\rightarrow$ Capsule Binding $\rightarrow$ Harvest

---

## 6. How Crop Selection Affects Alert Creation (Rules Engine)
Generic weather warnings are not enough for farmers. A wind speed of 15 km/h might be harmless for a mature tree, but disastrous for cotton flowers. The **Crop Advisory Rules Engine** combines **Consensus Weather + Selected Crop + Growth Stage** to create highly specific warnings.

Below are simple examples of how selection changes the alerts generated:

### Scenario A: Windy Weather (15 km/h Winds)
*   **If Crop is Cotton (Flowering stage)**:
    *   *Alert Generated*: **⚠️ Spraying: Critical Boll Shedding Risk (Danger)**.
    *   *Why*: Wind during flowering blows away chemical sprays, causes pesticide drift, and damages delicate flower buds.
    *   *Advice*: "DO NOT spray today. Delay all sprays until winds drop below 10 km/h."
*   **If Crop is Orange (Flowering stage)**:
    *   *Alert Generated*: **⚠️ Spraying: Fragile Blossom Damage Alert (Warning)**.
    *   *Why*: Orange blossoms are physically fragile; pressure from sprays combined with wind causes them to drop, reducing fruit yield.
    *   *Advice*: "Postpone chemical spraying or apply only at low pressure in the early morning."

### Scenario B: Rain Forecast (60% Rain Chance)
*   **If Crop is Rice/Paddy (Flowering stage)**:
    *   *Alert Generated*: **⚠️ Irrigation: Drain Excess Water (Warning)**.
    *   *Why*: Deep standing water during rice flowering washes away pollen, causing sterile grains.
    *   *Advice*: "Skip irrigation. Open drainage outlets to maintain field water depth below 5 cm."
*   **If Crop is Sesame (Any stage)**:
    *   *Alert Generated*: **⚠️ Irrigation: Sesame Waterlogging Danger (Danger)**.
    *   *Why*: Sesame has extremely delicate root systems. Standing water blocks root aeration and can kill the crop within 24 hours.
    *   *Advice*: "Stop irrigation immediately. Open field drainage channels to ensure swift runoff."
*   **If Crop is Potato (Tuber Bulking stage)**:
    *   *Alert Generated*: **⚠️ Irrigation: Tuber Rot Warning (Warning)**.
    *   *Why*: Saturated soils during bulking block soil oxygen, causing potato tubers to rot underground (hollow heart disease).
    *   *Advice*: "Skip irrigation. Ensure ridge-furrow drainage channels are completely free of weeds."
*   **If Crop is Onion (Bulb Development stage)**:
    *   *Alert Generated*: **⚠️ Irrigation: Bulb Splitting Risk (Warning)**.
    *   *Why*: Saturated soils cause onion bulbs to swell too rapidly, causing the outer scales to split and rot.
    *   *Advice*: "Stop irrigation. Let fields dry and clear water accumulation."

### Scenario C: Harvest Phase & Damp Conditions (High Humidity / Wet Weather)
*   **If Crop is Cotton (Harvest stage)**:
    *   *Alert Generated*: **⚠️ Harvesting: Cotton Lint Quality Risk (Danger)**.
    *   *Why*: Open cotton bolls absorb moisture, turning fiber yellow/brown and causing mold.
    *   *Advice*: "IMMEDIATELY stop cotton picking. Delay harvest until fibers are dry."
*   **If Crop is Sesame (Harvest stage)**:
    *   *Alert Generated*: **⚠️ Harvesting: Sesame Capsule Mildew Warning (Warning)**.
    *   *Why*: Cut sesame stalks are stacked upright in bundles to dry. Rain or high humidity causes mold/mildew to grow in the bundles, staining the seeds black.
    *   *Advice*: "Postpone cutting. Wait for a dry forecast window."

### Scenario D: Temperature & Humidity Fungal Alert
*   **If Crop is Potato (Vegetative/Growth stage)**:
    *   *Alert Generated*: **⚠️ Disease: Potato Late Blight Outbreak Risk (Danger)**.
    *   *Why*: Cool temperatures ($15^\circ\text{C} - 22^\circ\text{C}$) combined with extreme humidity ($>90\%$) are the exact incubation conditions for the devastating Late Blight fungus.
    *   *Advice*: "Inspect lower leaves for dark water-soaked spots. Apply protective fungicide immediately."
