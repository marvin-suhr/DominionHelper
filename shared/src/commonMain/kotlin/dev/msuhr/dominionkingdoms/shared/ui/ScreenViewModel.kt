package dev.msuhr.dominionkingdoms.shared.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Minimal ViewModel replacement for the shared UI (no androidx.lifecycle / Hilt on iOS). */
abstract class SharedViewModel {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)

    companion object {
        val SUBSCRIBED = SharingStarted.WhileSubscribed(5000)
        val EAGER = SharingStarted.Eagerly
    }
}

/** Port of the Android app's ScreenViewModel interface. */
interface ScreenViewModel {
    fun handleBackNavigation(): Boolean
    fun triggerScrollToTop()
    fun onSortTypeSelected(sortType: AppSortType)

    val currentAppSortType: StateFlow<AppSortType?>
    val showBackButton: StateFlow<Boolean>
    val showTopAppBar: StateFlow<Boolean>
}
