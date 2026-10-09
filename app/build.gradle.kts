import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.devtools.ksp)
  alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "com.example.openmeteoweather"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.openmeteoweather"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { load(it) }
            }
        }
        val imdEmail = localProperties.getProperty("IMD_EMAIL") ?: "akashdeep.sharma@swan.co.in"
        val imdPassword = localProperties.getProperty("IMD_PASSWORD") ?: "Swansat@2026"
        val imdApiKey = localProperties.getProperty("IMD_API_KEY") ?: "945e9d7d537f102dc81d8a32c12cdffbd686555ee754910e0d8706760ae2cc06"
        val ambeeApiKey = localProperties.getProperty("AMBEE_API_KEY") ?: "9f7a8a619dd65b11aa10468285df59276682e0c15d3e98ad8524ffabfc07ca47"
        val tomorrowApiKey = localProperties.getProperty("TOMORROW_API_KEY") ?: "Gc3EZAZN6rjZDsQIfPjjmAeY7LRGyTit"
        val openweatherApiKey = localProperties.getProperty("OPENWEATHER_API_KEY") ?: "19d5f51defd8706211bd1ae6f68a2d0c"

        buildConfigField("String", "IMD_EMAIL", "\"$imdEmail\"")
        buildConfigField("String", "IMD_PASSWORD", "\"$imdPassword\"")
        buildConfigField("String", "IMD_API_KEY", "\"$imdApiKey\"")
        buildConfigField("String", "AMBEE_API_KEY", "\"$ambeeApiKey\"")
        buildConfigField("String", "TOMORROW_API_KEY", "\"$tomorrowApiKey\"")
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$openweatherApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    @Suppress("DEPRECATION")
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}




dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.mockito:mockito-core:5.11.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Hilt Dependency Injection
  implementation(libs.google.dagger.hilt.android)
  ksp(libs.google.dagger.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)

  // Room Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Retrofit + OkHttp
  implementation(libs.retrofit2.retrofit)
  implementation(libs.retrofit2.converter.gson)
  implementation(libs.okhttp3.okhttp)
  implementation(libs.okhttp3.logging.interceptor)

  // Coil (Image Loading)
  implementation(libs.coil.compose)

  // GPS Location
  implementation(libs.play.services.location)

  // DataStore & Security Preferences
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.security.crypto)

  // Timber logging
  implementation("com.jakewharton.timber:timber:5.0.1")
}
