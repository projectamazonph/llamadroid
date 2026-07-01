package com.llamadroid.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.core.AppPreferences
import com.llamadroid.domain.models.DeviceTier
import com.llamadroid.domain.models.recommendedModels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val deviceTier: String = "detecting...",
    val isComplete: Boolean = false,
    val isLoading: Boolean = true,
    val pageCount: Int = 3
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Check if already completed
            appPreferences.onboardingComplete.collect { done ->
                if (done) { _state.value = _state.value.copy(isComplete = true); return@collect }
                val tier = DeviceTier.detect()
                _state.value = _state.value.copy(deviceTier = tier.label, isLoading = false)
            }
        }
    }

    fun nextPage() {
        val s = _state.value
        if (s.currentPage < s.pageCount - 1) {
            _state.value = s.copy(currentPage = s.currentPage + 1)
        } else {
            complete()
        }
    }

    fun skip() = complete()

    private fun complete() {
        viewModelScope.launch {
            appPreferences.setOnboardingComplete()
            _state.value = _state.value.copy(isComplete = true)
        }
    }
}
