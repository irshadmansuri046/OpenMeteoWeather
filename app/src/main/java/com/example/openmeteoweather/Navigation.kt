package com.example.openmeteoweather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.openmeteoweather.ui.home.HomeScreen
import com.example.openmeteoweather.ui.home.HomeViewModel
import com.example.openmeteoweather.ui.search.SearchScreen
import com.example.openmeteoweather.ui.search.SearchViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Home())

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Home> { key ->
                val homeViewModel: HomeViewModel = hiltViewModel()
                
                LaunchedEffect(key) {
                    homeViewModel.loadWeatherInfo(
                        latitude = key.latitude,
                        longitude = key.longitude,
                        name = key.cityName,
                        forceRefresh = false
                    )
                }

                HomeScreen(
                    viewModel = homeViewModel,
                    isCustomLocation = key.latitude != null && key.longitude != null,
                    onSearchClick = {
                        backStack.add(Search)
                    }
                )
            }
            entry<Search> {
                val searchViewModel: SearchViewModel = hiltViewModel()
                
                SearchScreen(
                    viewModel = searchViewModel,
                    onBackClick = {
                        backStack.removeLastOrNull()
                    },
                    onLocationSelected = { lat, lng, name ->
                        // Clear the Search screen and add/go back to Home with coordinates
                        backStack.removeLastOrNull()
                        backStack.add(Home(latitude = lat, longitude = lng, cityName = name))
                    }
                )
            }
        }
    )
}
