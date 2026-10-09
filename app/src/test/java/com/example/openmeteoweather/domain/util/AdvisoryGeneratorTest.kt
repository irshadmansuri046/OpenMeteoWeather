package com.example.openmeteoweather.domain.util

import com.example.openmeteoweather.domain.model.Advisory
import com.example.openmeteoweather.domain.model.AdvisoryType
import com.example.openmeteoweather.domain.model.SelectedCrop
import com.example.openmeteoweather.domain.model.SelectedCropStage
import com.example.openmeteoweather.domain.model.WeatherInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvisoryGeneratorTest {

    private fun createDefaultWeatherInfo(
        currentTemp: Double = 25.0,
        humidity: Double = 60.0,
        windSpeed: Double = 10.0,
        precipitation: Double = 0.0,
        rainProbability: Int = 20,
        uvIndex: Double = 4.0
    ): WeatherInfo {
        return WeatherInfo(
            latitude = 19.0760,
            longitude = 72.8777,
            currentTemp = currentTemp,
            feelsLikeTemp = currentTemp,
            weatherCode = 1,
            humidity = humidity,
            windSpeed = windSpeed,
            precipitation = precipitation,
            rainProbability = rainProbability,
            sunrise = "06:00",
            sunset = "18:30",
            uvIndex = uvIndex,
            time = "2026-06-01T12:00:00",
            isCached = false,
            lastCachedTime = 0L,
            hourlyForecast = emptyList(),
            dailyForecast = emptyList(),
            aqi = 50,
            pm25 = 12.0,
            pm10 = 20.0,
            providerName = "TestProvider"
        )
    }

    @Test
    fun testOrangeFloweringWindWarning() {
        val weather = createDefaultWeatherInfo(windSpeed = 15.0)
        val selectedCrop = SelectedCrop("orange", "Orange")
        val selectedStage = SelectedCropStage("orange_flowering", "Flowering")

        val advisories = AdvisoryGenerator.generateAdvisories(weather, selectedCrop, selectedStage)

        val sprayingAdvisories = advisories.filter { it.title.contains("Spraying") }
        assertEquals(1, sprayingAdvisories.size)
        assertEquals(AdvisoryType.WARNING, sprayingAdvisories[0].type)
        assertTrue(sprayingAdvisories[0].message.contains("blossom-drop"))
    }

    @Test
    fun testOrangeFruitDevelopmentHeatDropWarning() {
        val weather = createDefaultWeatherInfo(currentTemp = 39.0)
        val selectedCrop = SelectedCrop("orange", "Orange")
        val selectedStage = SelectedCropStage("orange_fruit_development", "Fruit Development")

        val advisories = AdvisoryGenerator.generateAdvisories(weather, selectedCrop, selectedStage)

        val heatAdvisories = advisories.filter { it.title.contains("Orange Heat-induced Fruit Drop") }
        assertEquals(1, heatAdvisories.size)
        assertEquals(AdvisoryType.DANGER, heatAdvisories[0].type)
    }

    @Test
    fun testPaddyFloweringRainWarning() {
        val weather = createDefaultWeatherInfo(rainProbability = 70, precipitation = 6.0)
        val selectedCrop = SelectedCrop("paddy", "Paddy")
        val selectedStage = SelectedCropStage("paddy_flowering", "Flowering")

        val advisories = AdvisoryGenerator.generateAdvisories(weather, selectedCrop, selectedStage)

        val irrigationAdvisories = advisories.filter { it.title.contains("Irrigation") }
        assertEquals(1, irrigationAdvisories.size)
        assertEquals(AdvisoryType.WARNING, irrigationAdvisories[0].type)
        assertTrue(irrigationAdvisories[0].message.contains("washes away pollen"))
    }

    @Test
    fun testPotatoTuberBulkingWaterloggingRotWarning() {
        val weather = createDefaultWeatherInfo(rainProbability = 65, precipitation = 6.0)
        val selectedCrop = SelectedCrop("potato", "Potato")
        val selectedStage = SelectedCropStage("potato_tuber_bulking", "Tuber Bulking")

        val advisories = AdvisoryGenerator.generateAdvisories(weather, selectedCrop, selectedStage)

        val rotAdvisories = advisories.filter { it.title.contains("Tuber Rot Warning") }
        assertEquals(1, rotAdvisories.size)
        assertEquals(AdvisoryType.WARNING, rotAdvisories[0].type)
    }

    @Test
    fun testPotatoLateBlightDiseaseRisk() {
        val weather = createDefaultWeatherInfo(humidity = 95.0, currentTemp = 18.0)
        val selectedCrop = SelectedCrop("potato", "Potato")
        val selectedStage = SelectedCropStage("potato_vegetative", "Vegetative")

        val advisories = AdvisoryGenerator.generateAdvisories(weather, selectedCrop, selectedStage)

        val diseaseAdvisories = advisories.filter { it.title.contains("Potato Late Blight") }
        assertEquals(1, diseaseAdvisories.size)
        assertEquals(AdvisoryType.DANGER, diseaseAdvisories[0].type)
    }

    @Test
    fun testOnionBulbDevelopmentRainWarning() {
        val weather = createDefaultWeatherInfo(rainProbability = 60, precipitation = 6.0)
        val selectedCrop = SelectedCrop("onion", "Onion")
        val selectedStage = SelectedCropStage("onion_bulb_development", "Bulb Development")

        val advisories = AdvisoryGenerator.generateAdvisories(weather, selectedCrop, selectedStage)

        val splittingAdvisories = advisories.filter { it.title.contains("Bulb splitting risk") }
        assertEquals(1, splittingAdvisories.size)
        assertEquals(AdvisoryType.WARNING, splittingAdvisories[0].type)
    }

    @Test
    fun testSesameWaterloggingDanger() {
        val weather = createDefaultWeatherInfo(rainProbability = 70, precipitation = 3.0)
        val selectedCrop = SelectedCrop("sesame", "Sesame")
        val selectedStage = SelectedCropStage("sesame_seedling", "Seedling")

        val advisories = AdvisoryGenerator.generateAdvisories(weather, selectedCrop, selectedStage)

        val sesameAdvisories = advisories.filter { it.title.contains("Sesame Waterlogging Danger") }
        assertEquals(1, sesameAdvisories.size)
        assertEquals(AdvisoryType.DANGER, sesameAdvisories[0].type)
    }

    @Test
    fun testSesameHarvestMildewWarning() {
        val weather = createDefaultWeatherInfo(humidity = 85.0, precipitation = 1.0)
        val selectedCrop = SelectedCrop("sesame", "Sesame")
        val selectedStage = SelectedCropStage("sesame_harvest", "Harvest")

        val advisories = AdvisoryGenerator.generateAdvisories(weather, selectedCrop, selectedStage)

        val mildewAdvisories = advisories.filter { it.title.contains("Sesame Capsule Mildew") }
        assertEquals(1, mildewAdvisories.size)
        assertEquals(AdvisoryType.WARNING, mildewAdvisories[0].type)
    }
}
