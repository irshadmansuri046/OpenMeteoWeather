package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.data.local.WeatherPreferences
import com.example.openmeteoweather.data.remote.swan.*
import com.example.openmeteoweather.domain.model.SelectedCrop
import com.example.openmeteoweather.domain.model.SelectedCropStage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SwanWeatherProviderTest {

    private class FakeSwanWeatherApiService(
        val customResponse: SwanCustomWeatherResponse
    ) : SwanWeatherApiService {
        override suspend fun getFullWeather(latitude: Double, longitude: Double): SwanWeatherResponse {
            throw UnsupportedOperationException()
        }

        override suspend fun getCrops(): List<SwanCropDto> {
            return emptyList()
        }

        override suspend fun getCropStages(cropId: Int): List<SwanCropStageDto> {
            return emptyList()
        }

        override suspend fun getCustomWeather(request: SwanCustomWeatherRequest): SwanCustomWeatherResponse {
            return customResponse
        }
    }

    @Test
    fun testGetCustomWeatherDataMapping() = runBlocking {
        // Arrange
        val mockAdvisory = SwanDailyAdvisoryDto(
            date = "2026-06-06",
            cropName = "Paddy (Rice)",
            stageName = "Sowing/Germination",
            alerts = listOf(
                SwanAlertDto(
                    ruleId = 25,
                    ruleName = "Paddy Sowing Alert",
                    parameter = "precipitation",
                    actualValue = "25.22",
                    thresholdValue = "15.0",
                    operator = ">",
                    severity = "WARNING",
                    advisoryText = "Favorable sowing conditions, monitor moisture."
                )
            )
        )

        val mockCustomResponse = SwanCustomWeatherResponse(
            status = 200,
            message = "Success",
            data = SwanCustomWeatherData(
                daily = listOf(
                    SwanParameterData(
                        parameter = "temperature",
                        rawKey = "t_2m:C",
                        height = "2m",
                        unit = "°C",
                        label = "TEMPERATURE",
                        coordinates = listOf(
                            SwanCoordinateData(
                                lat = 10.0,
                                lon = 76.0,
                                dates = listOf(
                                    SwanDateValue("2026-06-06T00:00:00Z", "27.08"),
                                    SwanDateValue("2026-06-06T01:00:00Z", "28.10")
                                )
                            )
                        )
                    ),
                    SwanParameterData(
                        parameter = "precipitation",
                        rawKey = "precip_24h:mm",
                        height = "",
                        unit = "mm",
                        label = "PRECIPITATION",
                        coordinates = listOf(
                            SwanCoordinateData(
                                lat = 10.0,
                                lon = 76.0,
                                dates = listOf(
                                    SwanDateValue("2026-06-06T00:00:00Z", "0.15"),
                                    SwanDateValue("2026-06-06T01:00:00Z", "0.0")
                                )
                            )
                        )
                    ),
                    SwanParameterData(
                        parameter = "relative_humidity",
                        rawKey = "relative_humidity_2m:p",
                        height = "2m",
                        unit = "%",
                        label = "HUMIDITY",
                        coordinates = listOf(
                            SwanCoordinateData(
                                lat = 10.0,
                                lon = 76.0,
                                dates = listOf(
                                    SwanDateValue("2026-06-06T00:00:00Z", "84.0"),
                                    SwanDateValue("2026-06-06T01:00:00Z", "80.0")
                                )
                            )
                        )
                    ),
                    SwanParameterData(
                        parameter = "wind_speed",
                        rawKey = "wind_speed_2m:kmh",
                        height = "2m",
                        unit = "km/h",
                        label = "WIND SPEED",
                        coordinates = listOf(
                            SwanCoordinateData(
                                lat = 10.0,
                                lon = 76.0,
                                dates = listOf(
                                    SwanDateValue("2026-06-06T00:00:00Z", "30.0"),
                                    SwanDateValue("2026-06-06T01:00:00Z", "25.0")
                                )
                            )
                        )
                    )
                ),
                weekly = listOf(
                    SwanParameterData(
                        parameter = "min_temperature",
                        rawKey = "t_min_2m_24h:C",
                        height = "2m_24h",
                        unit = "°C",
                        label = "MIN TEMPERATURE",
                        coordinates = listOf(
                            SwanCoordinateData(
                                lat = 10.0,
                                lon = 76.0,
                                dates = listOf(
                                    SwanDateValue("2026-06-06T06:30:00Z", "26.61")
                                )
                            )
                        )
                    ),
                    SwanParameterData(
                        parameter = "max_temperature",
                        rawKey = "t_max_2m_24h:C",
                        height = "2m_24h",
                        unit = "°C",
                        label = "MAX TEMPERATURE",
                        coordinates = listOf(
                            SwanCoordinateData(
                                lat = 10.0,
                                lon = 76.0,
                                dates = listOf(
                                    SwanDateValue("2026-06-06T06:30:00Z", "28.11")
                                )
                            )
                        )
                    )
                ),
                common = emptyList()
            ),
            advisories = listOf(mockAdvisory)
        )

        val fakeService = FakeSwanWeatherApiService(mockCustomResponse)
        val preferences: WeatherPreferences = mock()
        
        val testCrop = SelectedCrop("1", "Paddy (Rice)")
        val testStage = SelectedCropStage("10", "Sowing/Germination")
        
        whenever(preferences.selectedCrop).thenReturn(flowOf(testCrop))
        whenever(preferences.selectedCropStage).thenReturn(flowOf(testStage))

        val provider = SwanWeatherProvider(fakeService, preferences)

        // Act
        val result = provider.getWeatherData(10.0, 76.0)

        // Assert
        assertEquals(10.0, result.latitude, 0.001)
        assertEquals(76.0, result.longitude, 0.001)
        assertEquals(27.08, result.currentTemp, 0.001)
        assertEquals(84.0, result.humidity, 0.001)
        assertEquals(30.0, result.windSpeed, 0.001)
        assertEquals(0.15, result.precipitation, 0.001)
        assertEquals("2026-06-06T00:00", result.time)
        assertEquals("SwanWeather", result.providerName)

        // Hourly mapping checks
        assertEquals(2, result.hourlyForecast.size)
        assertEquals("2026-06-06T00:00", result.hourlyForecast[0].time)
        assertEquals(27.08, result.hourlyForecast[0].temp, 0.001)
        assertEquals(63, result.hourlyForecast[0].weatherCode) // precip > 0.1 -> 63

        // Daily mapping checks
        assertEquals(1, result.dailyForecast.size)
        assertEquals("2026-06-06", result.dailyForecast[0].date)
        assertEquals(26.61, result.dailyForecast[0].tempMin, 0.001)
        assertEquals(28.11, result.dailyForecast[0].tempMax, 0.001)

        // Custom Advisories JSON check
        assertTrue(!result.customAdvisoriesJson.isNullOrEmpty())
        assertTrue(result.customAdvisoriesJson!!.contains("Paddy Sowing Alert"))
    }
}
