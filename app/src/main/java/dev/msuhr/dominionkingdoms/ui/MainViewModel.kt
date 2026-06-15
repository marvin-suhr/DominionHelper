package dev.msuhr.dominionkingdoms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.msuhr.dominionkingdoms.data.CardDataUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val cardDataUpdater: CardDataUpdater,
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel() {

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    val isDarkMode: StateFlow<Boolean?> = userPrefsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val useSystemTheme: StateFlow<Boolean> = userPrefsRepository.useSystemTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showCardUpdateDialog: StateFlow<Boolean> = userPrefsRepository.showCardUpdateDialog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissCardUpdateDialog() {
        viewModelScope.launch {
            userPrefsRepository.setShowCardUpdateDialog(false)
        }
    }

    init {
        initializeApp()
    }

    private fun initializeApp() {
        // Run the database work safely off the Main thread using Dispatchers.IO
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cardDataUpdater.checkAndUpdateIfNeeded()
            } finally {
                // Ensure the UI unlocks even if something unexpected crashes during sync
                _isInitializing.value = false
            }
        }
    }
}