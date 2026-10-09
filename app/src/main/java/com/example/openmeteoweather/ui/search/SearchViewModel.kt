package com.example.openmeteoweather.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openmeteoweather.domain.model.LocationInfo


import com.example.openmeteoweather.domain.repository.WeatherRepository
import com.example.openmeteoweather.domain.usecase.GetRecentSearchesUseCase
import com.example.openmeteoweather.domain.usecase.SaveRecentSearchUseCase
import com.example.openmeteoweather.domain.usecase.SearchLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchLocationUseCase: SearchLocationUseCase,
    private val getRecentSearchesUseCase: GetRecentSearchesUseCase,
    private val saveRecentSearchUseCase: SaveRecentSearchUseCase,
    private val repository: WeatherRepository // For clear/delete helpers
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LocationInfo>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    val recentSearches: StateFlow<List<LocationInfo>> = getRecentSearchesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Automatically search as query changes with debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .filter { it.trim().length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.trim().isEmpty()) {
            _searchResults.value = emptyList()
            _errorMessage.value = null
        }
    }

    fun search() {
        performSearch(_searchQuery.value)
    }

    private fun performSearch(query: String) {
        if (query.trim().isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val results = searchLocationUseCase(query)
                _searchResults.value = results
                if (results.isEmpty()) {
                    _errorMessage.value = "No locations found for \"$query\""
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to search location: ${e.message ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectLocation(location: LocationInfo, onSelected: (LocationInfo) -> Unit) {
        viewModelScope.launch {
            saveRecentSearchUseCase(location)
            onSelected(location)
        }
    }

    fun deleteRecent(location: LocationInfo) {
        viewModelScope.launch {
            repository.deleteRecentSearch(location)
        }
    }

    fun clearRecent() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }
}
