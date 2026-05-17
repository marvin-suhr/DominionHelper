package dev.msuhr.dominionkingdoms.ui

import dev.msuhr.dominionkingdoms.model.AppSortType
import kotlinx.coroutines.flow.StateFlow

// Interface for the main viewmodels
interface ScreenViewModel {

    fun handleBackNavigation(): Boolean
    fun triggerScrollToTop()
    fun onSortTypeSelected(sortType: AppSortType)

    val currentAppSortType: StateFlow<AppSortType?>
    val showBackButton: StateFlow<Boolean>
    val showTopAppBar: StateFlow<Boolean>
}