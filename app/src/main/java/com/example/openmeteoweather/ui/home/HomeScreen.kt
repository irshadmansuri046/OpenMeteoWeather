package com.example.openmeteoweather.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openmeteoweather.domain.model.Crop
import com.example.openmeteoweather.domain.model.CropStage
import com.example.openmeteoweather.domain.model.ForecastDay
import com.example.openmeteoweather.domain.model.ForecastHour
import com.example.openmeteoweather.domain.model.ProviderForecastBreakdown
import com.example.openmeteoweather.domain.model.Advisory
import com.example.openmeteoweather.domain.model.AdvisoryType
import com.example.openmeteoweather.data.remote.swan.SwanDailyAdvisoryDto
import com.example.openmeteoweather.data.remote.swan.SwanAlertDto
import com.example.openmeteoweather.domain.util.AdvisoryGenerator
import com.example.openmeteoweather.domain.util.formatDecimal
import com.example.openmeteoweather.ui.components.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isCustomLocation: Boolean,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val cityName by viewModel.cityName.collectAsState()
    val selectedProvider by viewModel.selectedProvider.collectAsState()

    val cropSelectionState by viewModel.cropSelection.collectAsState()
    val (selectedCrop, selectedStage) = cropSelectionState
    val allCrops by viewModel.allCrops.collectAsState()

    var showCropSheet by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCrop) {
        if (selectedCrop == null) {
            showCropSheet = true
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Request Location Permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && !isCustomLocation) {
            viewModel.loadWeatherInfo(forceRefresh = true)
        }
    }

    LaunchedEffect(isCustomLocation) {
        if (!isCustomLocation) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val weatherInfo = state.weatherInfo
    val weatherCode = weatherInfo?.weatherCode ?: 0

    // Show failover warning Snackbar if active
    LaunchedEffect(weatherInfo) {
        val failoverAlert = weatherInfo?.alerts?.firstOrNull { 
            it.title.contains("Temporarily Unavailable")
        }
        if (failoverAlert != null) {
            snackbarHostState.showSnackbar(
                message = failoverAlert.description,
                duration = SnackbarDuration.Long
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(320.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Weather Settings",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.Black),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = Color.Black.copy(alpha = 0.12f)
                )
                
                Text(
                    text = "Weather Provider",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.Black.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                com.example.openmeteoweather.domain.provider.ProviderType.values().forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateWeatherProvider(provider)
                                scope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = {
                                viewModel.updateWeatherProvider(provider)
                                scope.launch { drawerState.close() }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.Black,
                                unselectedColor = Color.Black.copy(alpha = 0.6f)
                            )
                        )
                        Column {
                            Text(
                                text = when (provider) {
                                    com.example.openmeteoweather.domain.provider.ProviderType.OPEN_METEO -> "Open-Meteo"
                                    com.example.openmeteoweather.domain.provider.ProviderType.IMD -> "IMD Weather (India)"
                                    com.example.openmeteoweather.domain.provider.ProviderType.AMBEE -> "Ambee Forecast"
                                    com.example.openmeteoweather.domain.provider.ProviderType.TOMORROW_IO -> "Tomorrow.io"
                                    com.example.openmeteoweather.domain.provider.ProviderType.OPEN_WEATHER -> "OpenWeather"
                                    com.example.openmeteoweather.domain.provider.ProviderType.SWAN_WEATHER -> "SwanWeather"
                                    com.example.openmeteoweather.domain.provider.ProviderType.COMBINED_WEATHER -> "Combined Weather"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when (provider) {
                                    com.example.openmeteoweather.domain.provider.ProviderType.OPEN_METEO -> "Global forecast, high resolution"
                                    com.example.openmeteoweather.domain.provider.ProviderType.IMD -> "Local Indian forecasts & agro advisories"
                                    com.example.openmeteoweather.domain.provider.ProviderType.AMBEE -> "Hyperlocal forecast & environmental data"
                                    com.example.openmeteoweather.domain.provider.ProviderType.TOMORROW_IO -> "High accuracy global weather intelligence"
                                    com.example.openmeteoweather.domain.provider.ProviderType.OPEN_WEATHER -> "Global coverage, detailed hourly & alerts intelligence"
                                    com.example.openmeteoweather.domain.provider.ProviderType.SWAN_WEATHER -> "Fast and high availability production weather intelligence"
                                    com.example.openmeteoweather.domain.provider.ProviderType.COMBINED_WEATHER -> "Smart ensemble weather engine optimized for agriculture"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.6f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                val activeSourceLabel = when (selectedProvider) {
                    com.example.openmeteoweather.domain.provider.ProviderType.OPEN_METEO -> "Open-Meteo"
                    com.example.openmeteoweather.domain.provider.ProviderType.IMD -> "IMD India"
                    com.example.openmeteoweather.domain.provider.ProviderType.AMBEE -> "Ambee"
                    com.example.openmeteoweather.domain.provider.ProviderType.TOMORROW_IO -> "Tomorrow.io"
                    com.example.openmeteoweather.domain.provider.ProviderType.OPEN_WEATHER -> "OpenWeather"
                    com.example.openmeteoweather.domain.provider.ProviderType.SWAN_WEATHER -> "SwanWeather"
                    com.example.openmeteoweather.domain.provider.ProviderType.COMBINED_WEATHER -> "Combined Weather"
                }
                Text(
                    text = "Active Source: $activeSourceLabel",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    ) {
        WeatherBackground(weatherCode = weatherCode) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Menu",
                                    tint = Color.Black
                                )
                            }
                        },
                        title = {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = cityName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    )
                                }
                                Text(
                                    text = "Weather Source: ${weatherInfo?.providerName ?: "Loading..."}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(start = 20.dp)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search location",
                                    tint = Color.Black
                                )
                            }
                            IconButton(onClick = { viewModel.refreshWeather() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.Black
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.Black
                        )
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color.Transparent,
                modifier = modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        state.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                HomeWeatherShimmer()
                            }
                        }
                        state.error != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        tint = Color.Black.copy(alpha = 0.8f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = state.error ?: "Failed to load weather data",
                                        color = Color.Black,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Button(
                                        onClick = { viewModel.refreshWeather() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black.copy(alpha = 0.2f),
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier.border(1.dp, Color.Black, RoundedCornerShape(24.dp))
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        weatherInfo != null -> {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Crop & Growth Stage Status Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(Color.Black.copy(alpha = 0.05f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Eco,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "🌱 Crop: ${selectedCrop?.cropName ?: "Not Selected"}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Text(
                                                    text = "🌾 Stage: ${selectedStage?.stageName ?: "Not Selected"}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = Color.Black.copy(alpha = 0.7f),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                )
                                            }
                                        }
                                        TextButton(
                                            onClick = { showCropSheet = true },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                                        ) {
                                            Text(
                                                text = "Change",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                // 1. Current Weather Detail Card
                                CurrentWeatherHeader(weatherInfo = weatherInfo)

                                if (weatherInfo.confidenceLevel != null) {
                                    ConfidenceScoreCard(
                                        confidenceLevel = weatherInfo.confidenceLevel,
                                        agreementPercentage = weatherInfo.agreementPercentage ?: 100.0
                                    )
                                }

                                // Cache Alert if offline
                                if (weatherInfo.isCached) {
                                    CacheWarningCard(timestamp = weatherInfo.lastCachedTime)
                                }

                                // Active Weather Alerts
                                if (weatherInfo.alerts.isNotEmpty()) {
                                    WeatherAlertsSection(alerts = weatherInfo.alerts)
                                }

                                // 2. Farming Advisory Panel
                                if (weatherInfo.providerName == "SwanWeather" && !weatherInfo.customAdvisoriesJson.isNullOrEmpty()) {
                                    SwanCalendarAdvisoryPanel(
                                        customAdvisoriesJson = weatherInfo.customAdvisoriesJson,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    val advisories = remember(weatherInfo, selectedCrop, selectedStage) {
                                        AdvisoryGenerator.generateAdvisories(weatherInfo, selectedCrop, selectedStage)
                                    }
                                    FarmingAdvisoriesSection(advisories = advisories)
                                }

                                // 3. Hourly Forecast Card
                                HourlyForecastCard(hourlyList = weatherInfo.hourlyForecast)

                                // 4. 7-Day Forecast Card
                                WeeklyForecastCard(weeklyList = weatherInfo.dailyForecast)

                                // 5. Grid of Details (Humidity, Wind, UV, Sunrise/Sunset, AQI)
                                Text(
                                    text = "Weather Details",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                WeatherMetricsGrid(weather = weatherInfo)

                                if (weatherInfo.providerBreakdownsJson != null) {
                                    ForecastSourceComparisonCard(
                                        breakdownsJson = weatherInfo.providerBreakdownsJson,
                                        consensusTemp = weatherInfo.currentTemp,
                                        consensusRainProb = weatherInfo.rainProbability,
                                        consensusWindSpeed = weatherInfo.windSpeed
                                    )
                                }
                                
                                if (weatherInfo.providerName.contains("Ambee", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    AmbeeEnvironmentalIntelligenceSection(weather = weatherInfo)
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCropSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // Allow dismiss only if a crop is already selected.
                if (selectedCrop != null) {
                    showCropSheet = false
                }
            },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            containerColor = Color.White
        ) {
            var tempSelectedCrop by remember { mutableStateOf<Crop?>(selectedCrop?.let { Crop(it.cropId, it.cropName) }) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (tempSelectedCrop == null) "Select Your Crop" else "Select Growth Stage",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (tempSelectedCrop != null && selectedCrop != null) {
                        IconButton(
                            onClick = { tempSelectedCrop = null }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back to Crops",
                                tint = Color.Black
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.Black.copy(alpha = 0.12f))

                if (tempSelectedCrop == null) {
                    // Page 1: Crop List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        allCrops.forEach { crop ->
                            val isSelected = selectedCrop?.cropId == crop.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color.Black.copy(alpha = 0.05f) 
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        tempSelectedCrop = crop
                                        viewModel.selectCrop(crop)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Eco,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF4CAF50) else Color.Black.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = crop.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = Color.Black,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Black.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                } else {
                    // Page 2: Growth Stage List
                    val stagesFlow = remember(tempSelectedCrop) { viewModel.getStagesForCrop(tempSelectedCrop!!.id) }
                    val stages by stagesFlow.collectAsState(initial = emptyList())

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        stages.forEach { stage ->
                            val isSelected = selectedStage?.stageId == stage.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color.Black.copy(alpha = 0.05f) 
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectStage(stage)
                                        showCropSheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Grass,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF4CAF50) else Color.Black.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = stage.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = Color.Black,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CurrentWeatherHeader(
    weatherInfo: com.example.openmeteoweather.domain.model.WeatherInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = WeatherCodeUtils.getWeatherIcon(weatherInfo.weatherCode),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${weatherInfo.currentTemp.toInt()}°",
            style = MaterialTheme.typography.displayLarge.copy(
                color = Color.Black,
                fontWeight = FontWeight.Light,
                fontSize = 72.sp
            )
        )
        Text(
            text = WeatherCodeUtils.getWeatherDescription(weatherInfo.weatherCode),
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Feels like ${weatherInfo.feelsLikeTemp.toInt()}°  •  Rain: ${weatherInfo.rainProbability}%",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Black.copy(alpha = 0.8f)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = Color.Black.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Source,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.9f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Weather Source: ${weatherInfo.providerName}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun CacheWarningCard(
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val formattedTime = format.format(date)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF9C4)) // Soft yellow alert
            .border(1.dp, Color(0xFFFBC02D), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.WifiOff,
            contentDescription = "Offline Cache",
            tint = Color(0xFFF57F17)
        )
        Text(
            text = "Showing offline weather cache from $formattedTime. Check internet connection.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF5D4037)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FarmingAdvisoriesSection(
    advisories: List<com.example.openmeteoweather.domain.model.Advisory>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Farming Advisories",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        )
        advisories.forEach { advisory ->
            AdvisoryCard(advisory = advisory)
        }
    }
}

@Composable
fun HourlyForecastCard(
    hourlyList: List<ForecastHour>,
    modifier: Modifier = Modifier
) {
    val displayedHourly = hourlyList.take(24) // limit to next 24 hours
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Hourly Forecast",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.Black.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayedHourly) { hour ->
                    HourlyItem(hour = hour)
                }
            }
        }
    }
}

@Composable
fun HourlyItem(
    hour: ForecastHour,
    modifier: Modifier = Modifier
) {
    val hourText = try {
        // Hour format from 2026-05-25T14:00
        val parts = hour.time.split("T")
        if (parts.size == 2) parts[1] else hour.time
    } catch (e: Exception) {
        hour.time
    }

    Column(
        modifier = modifier.width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = hourText,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.Black.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        )
        Icon(
            imageVector = WeatherCodeUtils.getWeatherIcon(hour.weatherCode),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "${hour.temp.toInt()}°",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        )
        if (hour.rainProbability > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "${hour.rainProbability}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64B5F6),
                        fontSize = 9.sp
                    )
                )
            }
        }
    }
}

@Composable
fun WeeklyForecastCard(
    weeklyList: List<ForecastDay>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "7-Day Forecast",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.Black.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            )
            weeklyList.forEach { day ->
                WeeklyItem(day = day)
            }
        }
    }
}

@Composable
fun WeeklyItem(
    day: ForecastDay,
    modifier: Modifier = Modifier
) {
    val dayText = try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(day.date)
        if (date != null) {
            val formatter = SimpleDateFormat("EEEE", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = date
            val today = Calendar.getInstance()
            if (calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                "Today"
            } else {
                formatter.format(date)
            }
        } else {
            day.date
        }
    } catch (e: Exception) {
        day.date
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Black,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1.5f)
        )
        Row(
            modifier = Modifier.weight(2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = WeatherCodeUtils.getWeatherIcon(day.weatherCode),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = WeatherCodeUtils.getWeatherDescription(day.weatherCode),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Black.copy(alpha = 0.7f),
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier.weight(1.2f),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${day.tempMax.toInt()}°",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${day.tempMin.toInt()}°",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
fun WeatherAlertsSection(
    alerts: List<com.example.openmeteoweather.domain.model.WeatherAlert>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Active Weather Alerts",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        )
        alerts.forEach { alert ->
            WeatherAlertCard(alert = alert)
        }
    }
}

@Composable
fun WeatherAlertCard(
    alert: com.example.openmeteoweather.domain.model.WeatherAlert,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, borderColor, iconColor) = when (alert.severity.uppercase(Locale.ROOT)) {
        "DANGER" -> Triple(Color(0xFFFFEBEE), Color(0xFFEF9A9A), Icons.Rounded.Warning)
        "WARNING" -> Triple(Color(0xFFFFF3E0), Color(0xFFFFCC80), Icons.Rounded.Warning)
        else -> Triple(Color(0xFFE3F2FD), Color(0xFF90CAF9), Icons.Rounded.Info)
    }
    val textColor = when (alert.severity.uppercase(Locale.ROOT)) {
        "DANGER" -> Color(0xFFB71C1C)
        "WARNING" -> Color(0xFFE65100)
        else -> Color(0xFF0D47A1)
    }
    val descColor = when (alert.severity.uppercase(Locale.ROOT)) {
        "DANGER" -> Color(0xFF5D0000)
        "WARNING" -> Color(0xFF5D2D00)
        else -> Color(0xFF002D5D)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor.copy(alpha = 0.9f))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = iconColor,
            contentDescription = alert.severity,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = alert.timing,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = textColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = descColor
                )
            )
        }
    }
}

@Composable
fun AmbeeEnvironmentalIntelligenceSection(
    weather: com.example.openmeteoweather.domain.model.WeatherInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ambee Environmental Intelligence",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        )

        // 1. Detailed Air Quality Index Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Air,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Detailed Air Quality",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "AQI: ${weather.aqi ?: "N/A"}", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, fontWeight = FontWeight.Bold))
                        Text(text = "PM2.5: ${weather.pm25.formatDecimal()} µg/m³", style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.7f)))
                        Text(text = "PM10: ${weather.pm10.formatDecimal()} µg/m³", style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.7f)))
                    }
                    Column {
                        Text(text = "CO: ${weather.co.formatDecimal()} ppm", style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.7f)))
                        Text(text = "NO2: ${weather.no2.formatDecimal()} ppb", style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.7f)))
                        Text(text = "O3: ${weather.ozone.formatDecimal()} ppm", style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.7f)))
                        Text(text = "SO2: ${weather.so2.formatDecimal()} ppb", style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.7f)))
                    }
                }
            }
        }

        // 2. Soil Moisture & Temperature Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Soil Moisture",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.Black.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        )
                    }
                    Text(
                        text = "${weather.soilMoisture.formatDecimal()}%",
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.Black, fontSize = 20.sp)
                    )
                    val moistureStatus = if ((weather.soilMoisture ?: 0.0) < 25.0) "Low (Irrigate)" else "Optimal"
                    val moistureStatusColor = if ((weather.soilMoisture ?: 0.0) < 25.0) Color(0xFFE57373) else Color(0xFF81C784)
                    Text(
                        text = moistureStatus,
                        style = MaterialTheme.typography.bodySmall.copy(color = moistureStatusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Thermostat,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Soil Temp",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.Black.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        )
                    }
                    Text(
                        text = "${weather.soilTemperature.formatDecimal()}°C",
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.Black, fontSize = 20.sp)
                    )
                    Text(
                        text = "Root Zone",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Black.copy(alpha = 0.6f), fontSize = 11.sp)
                    )
                }
            }
        }

        // 3. Crop Stress & Risk Insights
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Eco,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Crop Health & Environmental Risk",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                val temp = weather.currentTemp
                val aqiVal = weather.aqi ?: 0
                val wind = weather.windSpeed
                val moisture = weather.soilMoisture ?: 0.0

                val heatStressStatus = if (temp >= 35.0) "High Heat Stress Expected" else "Normal Heat stress"
                val sprayingStatus = if (aqiVal > 100) "Poor Air Quality (Spraying Warning)" else if (wind > 25.0) "High Wind (Drift Risk)" else "Ideal Spraying conditions"
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Heat Stress Indicator:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black.copy(alpha = 0.7f)))
                        Text(text = heatStressStatus, style = MaterialTheme.typography.bodyMedium.copy(color = if (temp >= 35.0) Color(0xFFFFB74D) else Color(0xFF81C784), fontWeight = FontWeight.Bold))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Spraying Activity Safety:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black.copy(alpha = 0.7f)))
                        Text(text = sprayingStatus, style = MaterialTheme.typography.bodyMedium.copy(color = if (aqiVal > 100 || wind > 25.0) Color(0xFFE57373) else Color(0xFF81C784), fontWeight = FontWeight.Bold))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Environmental Risk Index:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black.copy(alpha = 0.7f)))
                        val riskScore = (if (temp >= 35.0) 30 else 0) + (if (aqiVal > 100) 40 else 10) + (if (moisture < 25.0) 30 else 0)
                        val riskText = when {
                            riskScore >= 70 -> "High Risk ($riskScore/100)"
                            riskScore >= 40 -> "Moderate Risk ($riskScore/100)"
                            else -> "Low Risk ($riskScore/100)"
                        }
                        val riskColor = when {
                            riskScore >= 70 -> Color(0xFFE57373)
                            riskScore >= 40 -> Color(0xFFFFB74D)
                            else -> Color(0xFF81C784)
                        }
                        Text(text = riskText, style = MaterialTheme.typography.bodyMedium.copy(color = riskColor, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun ConfidenceScoreCard(
    confidenceLevel: String,
    agreementPercentage: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.05f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast Confidence: $confidenceLevel",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Providers Agreement: ${agreementPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
fun ForecastSourceComparisonCard(
    breakdownsJson: String,
    consensusTemp: Double,
    consensusRainProb: Int,
    consensusWindSpeed: Double
) {
    val gson = remember { Gson() }
    val breakdowns = remember(breakdownsJson) {
        try {
            val listType = object : TypeToken<List<ProviderForecastBreakdown>>() {}.type
            gson.fromJson<List<ProviderForecastBreakdown>>(breakdownsJson, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    if (breakdowns.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Compare,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Forecast Source Comparison",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Show Less" else "Show More",
                    tint = Color.Black
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Provider", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Temp", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black), modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                        Text(text = "Rain %", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black), modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                        Text(text = "Wind", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black), modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
                    }
                }

                HorizontalDivider(color = Color.Black.copy(alpha = 0.12f), modifier = Modifier.padding(bottom = 8.dp))

                // Table Rows
                breakdowns.forEach { breakdown ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = breakdown.providerName,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (breakdown.isSuccess) {
                                Text(
                                    text = "${breakdown.temp.toInt()}°C",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                                    modifier = Modifier.width(55.dp),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "${breakdown.rainProbability}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                                    modifier = Modifier.width(55.dp),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "${breakdown.windSpeed.toInt()} km/h",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.End
                                )
                            } else {
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Red),
                                    modifier = Modifier.width(186.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.Black.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 8.dp))

                // Consensus Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Consensus Forecast",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${consensusTemp.toInt()}°C",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                            modifier = Modifier.width(55.dp),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "$consensusRainProb%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                            modifier = Modifier.width(55.dp),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "${consensusWindSpeed.toInt()} km/h",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

