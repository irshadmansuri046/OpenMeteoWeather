package com.example.openmeteoweather.data.remote.openweather

import com.google.gson.annotations.SerializedName

data class OpenWeatherOneCallResponse(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("timezone_offset") val timezoneOffset: Long?,
    @SerializedName("current") val current: CurrentWeather?,
    @SerializedName("hourly") val hourly: List<HourlyForecast>?,
    @SerializedName("daily") val daily: List<DailyForecast>?,
    @SerializedName("alerts") val alerts: List<WeatherAlertDto>?
)

data class CurrentWeather(
    @SerializedName("dt") val dt: Long,
    @SerializedName("sunrise") val sunrise: Long?,
    @SerializedName("sunset") val sunset: Long?,
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("pressure") val pressure: Int?,
    @SerializedName("humidity") val humidity: Double,
    @SerializedName("dew_point") val dewPoint: Double?,
    @SerializedName("uvi") val uvi: Double?,
    @SerializedName("clouds") val clouds: Double?,
    @SerializedName("visibility") val visibility: Double?,
    @SerializedName("wind_speed") val windSpeed: Double,
    @SerializedName("wind_deg") val windDeg: Double?,
    @SerializedName("weather") val weather: List<WeatherCondition>?
)

data class HourlyForecast(
    @SerializedName("dt") val dt: Long,
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double?,
    @SerializedName("pressure") val pressure: Int?,
    @SerializedName("humidity") val humidity: Double?,
    @SerializedName("dew_point") val dewPoint: Double?,
    @SerializedName("uvi") val uvi: Double?,
    @SerializedName("clouds") val clouds: Double?,
    @SerializedName("visibility") val visibility: Double?,
    @SerializedName("wind_speed") val windSpeed: Double?,
    @SerializedName("wind_deg") val windDeg: Double?,
    @SerializedName("weather") val weather: List<WeatherCondition>?,
    @SerializedName("pop") val pop: Double? // Probability of precipitation (0.0 to 1.0)
)

data class DailyForecast(
    @SerializedName("dt") val dt: Long,
    @SerializedName("sunrise") val sunrise: Long?,
    @SerializedName("sunset") val sunset: Long?,
    @SerializedName("temp") val temp: TempValues?,
    @SerializedName("feels_like") val feelsLike: FeelsLikeValues?,
    @SerializedName("pressure") val pressure: Int?,
    @SerializedName("humidity") val humidity: Double?,
    @SerializedName("dew_point") val dewPoint: Double?,
    @SerializedName("wind_speed") val windSpeed: Double?,
    @SerializedName("wind_deg") val windDeg: Double?,
    @SerializedName("weather") val weather: List<WeatherCondition>?,
    @SerializedName("clouds") val clouds: Double?,
    @SerializedName("pop") val pop: Double?,
    @SerializedName("uvi") val uvi: Double?
)

data class TempValues(
    @SerializedName("day") val day: Double,
    @SerializedName("min") val min: Double,
    @SerializedName("max") val max: Double,
    @SerializedName("night") val night: Double,
    @SerializedName("eve") val eve: Double,
    @SerializedName("morn") val morn: Double
)

data class FeelsLikeValues(
    @SerializedName("day") val day: Double,
    @SerializedName("night") val night: Double,
    @SerializedName("eve") val eve: Double,
    @SerializedName("morn") val morn: Double
)

data class WeatherCondition(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("icon") val icon: String?
)

data class WeatherAlertDto(
    @SerializedName("sender_name") val senderName: String?,
    @SerializedName("event") val event: String?,
    @SerializedName("start") val start: Long,
    @SerializedName("end") val end: Long,
    @SerializedName("description") val description: String?,
    @SerializedName("tags") val tags: List<String>?
)

data class OpenWeatherCurrentResponse(
    @SerializedName("coord") val coord: CoordDto?,
    @SerializedName("weather") val weather: List<WeatherCondition>?,
    @SerializedName("main") val main: MainDto?,
    @SerializedName("wind") val wind: WindDto?,
    @SerializedName("clouds") val clouds: CloudsDto?,
    @SerializedName("dt") val dt: Long,
    @SerializedName("sys") val sys: SysDto?,
    @SerializedName("timezone") val timezone: Long?,
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?
)

data class CoordDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)

data class MainDto(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("pressure") val pressure: Int?,
    @SerializedName("humidity") val humidity: Double
)

data class WindDto(
    @SerializedName("speed") val speed: Double,
    @SerializedName("deg") val deg: Double?
)

data class CloudsDto(
    @SerializedName("all") val all: Int?
)

data class SysDto(
    @SerializedName("sunrise") val sunrise: Long?,
    @SerializedName("sunset") val sunset: Long?
)

data class OpenWeatherForecastResponse(
    @SerializedName("cod") val cod: String?,
    @SerializedName("message") val message: Double?,
    @SerializedName("cnt") val cnt: Int?,
    @SerializedName("list") val list: List<ForecastItem>?,
    @SerializedName("city") val city: CityDto?
)

data class ForecastItem(
    @SerializedName("dt") val dt: Long,
    @SerializedName("main") val main: MainDto?,
    @SerializedName("weather") val weather: List<WeatherCondition>?,
    @SerializedName("clouds") val clouds: CloudsDto?,
    @SerializedName("wind") val wind: WindDto?,
    @SerializedName("pop") val pop: Double?,
    @SerializedName("dt_txt") val dtTxt: String?
)

data class CityDto(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("coord") val coord: CoordDto?,
    @SerializedName("country") val country: String?,
    @SerializedName("sunrise") val sunrise: Long?,
    @SerializedName("sunset") val sunset: Long?
)
