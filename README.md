# OpenMeteoWeather - Android Weather App POC

A modern Android Weather Application POC built in Kotlin using Jetpack Compose, MVVM architecture, and Clean Architecture principles. This component is designed to be highly modular and reusable for future integration into the **KhetPe** agriculture app.

---

## 🛠 Tech Stack

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Architecture**: MVVM with Clean Architecture structure (`data`, `domain`, `ui`, `di`)
*   **Networking**: [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/) (Logging Interceptor)
*   **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
*   **Database (Offline Cache)**: [Room](https://developer.android.com/training/data-storage/room)
*   **Asynchronous programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
*   **Location**: Google Play Services Location (GPS coordinates resolution)
*   **Design System**: Material 3 (with custom Forest Green/Olive/Wheat Earth tones)
*   **API**: [Open-Meteo Weather API](https://open-meteo.com/) (No API Key Required)

---

## 🌿 Key Features

1.  **Home Weather Dashboard**:
    *   Dynamic background gradients that change based on current weather conditions (e.g. warm gold for Sunny, slate blue-grey for Rain, stormy charcoal for Thunderstorms).
    *   Detailed weather metrics (Temperature, Feels Like, Wind Speed, Humidity, Precipitation Probability, UV Index, Sunrise, Sunset).
    *   Air Quality Index (AQI) dashboard integration displaying PM2.5, PM10, and US-AQI status.
2.  **Farmer-Friendly UI**:
    *   Large, highly readable typography and high-contrast earth-tone cards.
    *   Clean layout optimized for outdoor visibility.
3.  **Agriculture-Specific Insights**:
    *   Runs weather metrics through an `AdvisoryGenerator` rules engine.
    *   Provides simple, actionable farming warnings (e.g. *"Strong winds expected. Avoid pesticide spraying to prevent chemical drift"* or *"Rain expected. Avoid irrigation today"*).
4.  **Hourly & 7-Day Forecast**:
    *   Horizontal scrollable row for the next 24 hours of weather forecasts.
    *   Vertical listing of day-wise cards for the next 7 days including UV index levels.
5.  **Search & History Location Management**:
    *   Search cities and farming regions using the Open-Meteo Geocoding API.
    *   Saves recent searches locally in the database for instant recall.
6.  **Offline Support**:
    *   Every successful network fetch caches the weather forecast and AQI in Room DB (grouped/approximated by coordinates).
    *   If the user has no internet access, the app automatically serves the last cached data, displaying a friendly warning message: *"Showing offline weather cache from [Time]"*.

---

## 📁 Project Directory Structure

```
com.example.openmeteoweather/
│
├── data/
│   ├── local/
│   │   ├── WeatherDatabase.kt           # Room Database definition
│   │   ├── WeatherDao.kt                # Database operations for cache & history
│   │   ├── WeatherEntity.kt             # Cached weather & AQI entity
│   │   └── SearchHistoryEntity.kt       # Saved locations history
│   │
│   ├── remote/
│   │   ├── WeatherApi.kt                # Retrofit interface for weather
│   │   ├── AirQualityApi.kt             # Retrofit interface for Air Quality
│   │   ├── GeocodingApi.kt              # Retrofit interface for geocoding
│   │   └── dto/
│   │       ├── WeatherResponseDto.kt    # Weather DTO models
│   │       ├── AirQualityResponseDto.kt # AQI DTO models
│   │       └── GeocodingResponseDto.kt  # Geocoding search DTO models
│   │
│   ├── repository/
│   │   └── WeatherRepositoryImpl.kt     # Concrete repository handling caching & fallbacks
│   │
│   └── location/
│       └── DefaultLocationTracker.kt    # GPS location client implementation
│
├── domain/
│   ├── model/
│   │   ├── WeatherInfo.kt               # Domain model representing current weather
│   │   ├── ForecastDay.kt               # Daily forecast model
│   │   ├── ForecastHour.kt              # Hourly forecast model
│   │   ├── LocationInfo.kt              # Geocoding location model
│   │   └── Advisory.kt                  # Farming Advisory model
│   │
│   ├── repository/
│   │   └── WeatherRepository.kt         # Repository interface definition
│   │
│   ├── usecase/
│   │   ├── GetWeatherUseCase.kt         # Use case to fetch weather
│   │   ├── SearchLocationUseCase.kt     # Use case to search cities
│   │   ├── GetRecentSearchesUseCase.kt  # Use case to fetch search history
│   │   └── SaveRecentSearchUseCase.kt   # Use case to cache location selection
│   │
│   ├── location/
│   │   └── LocationTracker.kt           # Location Tracker interface
│   │
│   └── util/
│       └── AdvisoryGenerator.kt         # Agricultural advice generator rules engine
│
├── di/
│   └── AppModule.kt                     # Hilt module (Networking, Databases, repositories)
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                     # Forest green / earth-tone palettes
│   │   ├── Theme.kt                     # Compose Theme configuration
│   │   └── Type.kt                      # Typography setups
│   │
│   ├── components/
│   │   ├── WeatherBackground.kt         # Gradients based on weather code
│   │   ├── ShimmerPlaceholder.kt        # Loading effects
│   │   ├── WeatherMetricsGrid.kt        # Humidity, wind, sunrise/sunset metrics
│   │   ├── AdvisoryCard.kt              # Earth-toned advisory displays
│   │   └── WeatherCodeUtils.kt          # Maps WMO weather codes to text, gradients & icons
│   │
│   ├── home/
│   │   ├── HomeScreen.kt                # Dashboard layout (hourly, weekly, advisories)
│   │   └── HomeViewModel.kt             # Handles current location, caching & loading state
│   │
│   └── search/
│       ├── SearchScreen.kt              # City query input, search results & history list
│       └── SearchViewModel.kt           # Handles geocoding API calls and history
│
├── MainActivity.kt                      # Bootstraps Jetpack Compose and Theme
├── Navigation.kt                        # Manages navigation between Home and Search
├── NavigationKeys.kt                    # Safe route keys for Navigation
└── WeatherApplication.kt                # Initiates Dagger Hilt
```

---

## 🚀 How to Setup and Run

### Prerequisites
1.  **Android Studio** (Koala / Ladybug or newer recommended).
2.  **JDK 17** configured in your system and Android Studio.
3.  An Android device or emulator running **Android 8.0 (API 26) or higher** (API 34 compileSdk is configured).

### Steps
1.  Clone or download the project files into a folder.
2.  Open Android Studio, select **File -> Open**, and choose the project directory (`OpenMeteoWeather`).
3.  Let Gradle sync finish. Android Studio will automatically download all required dependencies.
4.  Run the application on an emulator or a physical device using the **Run** button (green play icon) in Android Studio.
5.  To verify build and compilation via the command line, run:
    ```bash
    ./gradlew assembleDebug
    ```
