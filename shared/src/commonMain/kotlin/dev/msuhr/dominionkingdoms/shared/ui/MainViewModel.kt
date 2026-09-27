package dev.msuhr.dominionkingdoms.shared.ui

import dev.msuhr.dominionkingdoms.shared.data.AppPrefsSource
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Theme preferences holder (port of the relevant parts of Android's MainViewModel). */
class MainViewModel(
    userPrefsRepository: AppPrefsSource
) : SharedViewModel() {

    /** null = follow system, true = dark, false = light. */
    val isDarkMode: StateFlow<Boolean?> = userPrefsRepository.isDarkMode
        .stateIn(scope, SUBSCRIBED, null)

    val useSystemTheme: StateFlow<Boolean> = userPrefsRepository.useSystemTheme
        .stateIn(scope, SUBSCRIBED, true)
}
