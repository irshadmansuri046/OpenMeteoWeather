package com.example.openmeteoweather.di

import android.content.Context
import androidx.room.Room
import com.example.openmeteoweather.data.local.WeatherDao
import com.example.openmeteoweather.data.local.WeatherDatabase
import com.example.openmeteoweather.data.remote.AirQualityApi
import com.example.openmeteoweather.data.remote.GeocodingApi
import com.example.openmeteoweather.data.remote.WeatherApi
import com.example.openmeteoweather.data.repository.WeatherRepositoryImpl
import com.example.openmeteoweather.domain.repository.WeatherRepository
import com.example.openmeteoweather.data.local.CropDao
import com.example.openmeteoweather.domain.repository.CropRepository
import com.example.openmeteoweather.data.repository.CropRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Named
import android.app.Application
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.example.openmeteoweather.domain.location.LocationTracker
import com.example.openmeteoweather.data.location.DefaultLocationTracker

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    @Named("imd_auth_client")
    fun provideImdAuthOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: com.example.openmeteoweather.data.remote.imd.IMDAuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("ambee_client")
    fun provideAmbeeOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        ambeeInterceptor: com.example.openmeteoweather.data.remote.ambee.AmbeeAuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(ambeeInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAmbeeApiService(@Named("ambee_client") okHttpClient: OkHttpClient): com.example.openmeteoweather.data.remote.ambee.AmbeeApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.ambeedata.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.example.openmeteoweather.data.remote.ambee.AmbeeApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("tomorrow_client")
    fun provideTomorrowOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        tomorrowInterceptor: com.example.openmeteoweather.data.remote.tomorrow.TomorrowAuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(tomorrowInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideTomorrowApiService(@Named("tomorrow_client") okHttpClient: OkHttpClient): com.example.openmeteoweather.data.remote.tomorrow.TomorrowApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.tomorrow.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.example.openmeteoweather.data.remote.tomorrow.TomorrowApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("openweather_client")
    fun provideOpenWeatherOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        openWeatherInterceptor: com.example.openmeteoweather.data.remote.openweather.OpenWeatherAuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(openWeatherInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenWeatherApiService(@Named("openweather_client") okHttpClient: OkHttpClient): com.example.openmeteoweather.data.remote.openweather.OpenWeatherApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.example.openmeteoweather.data.remote.openweather.OpenWeatherApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("swan_client")
    fun provideSwanWeatherOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideSwanWeatherApiService(@Named("swan_client") okHttpClient: OkHttpClient): com.example.openmeteoweather.data.remote.swan.SwanWeatherApiService {
        return Retrofit.Builder()
            .baseUrl("https://swanweather-production.up.railway.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.example.openmeteoweather.data.remote.swan.SwanWeatherApiService::class.java)
    }


    @Provides
    @Singleton
    fun provideWeatherApi(okHttpClient: OkHttpClient): WeatherApi {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAirQualityApi(okHttpClient: OkHttpClient): AirQualityApi {
        return Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AirQualityApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeocodingApi(okHttpClient: OkHttpClient): GeocodingApi {
        return Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideIMDAuthService(@Named("imd_auth_client") okHttpClient: OkHttpClient): com.example.openmeteoweather.data.remote.imd.IMDAuthService {
        return Retrofit.Builder()
            .baseUrl("https://api.imd.gov.in/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.example.openmeteoweather.data.remote.imd.IMDAuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideIMDApiService(okHttpClient: OkHttpClient): com.example.openmeteoweather.data.remote.imd.IMDApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.imd.gov.in/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.example.openmeteoweather.data.remote.imd.IMDApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherDatabase(@ApplicationContext context: Context): WeatherDatabase {
        return Room.databaseBuilder(
            context,
            WeatherDatabase::class.java,
            "weather_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideWeatherDao(db: WeatherDatabase): WeatherDao {
        return db.dao
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(
        impl: WeatherRepositoryImpl
    ): WeatherRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(app: Application): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(app)
    }

    @Provides
    @Singleton
    fun provideLocationTracker(
        fusedLocationProviderClient: FusedLocationProviderClient,
        app: Application
    ): LocationTracker {
        return DefaultLocationTracker(fusedLocationProviderClient, app)
    }

    @Provides
    @Singleton
    fun provideCropDao(db: WeatherDatabase): CropDao {
        return db.cropDao
    }

    @Provides
    @Singleton
    fun provideCropRepository(
        impl: CropRepositoryImpl
    ): CropRepository {
        return impl
    }
}
