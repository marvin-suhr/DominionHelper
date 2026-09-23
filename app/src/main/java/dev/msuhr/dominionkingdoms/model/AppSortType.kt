package dev.msuhr.dominionkingdoms.model

import dev.msuhr.dominionkingdoms.ui.LibraryViewModel

sealed class AppSortType(val text: String) {
    data class Kingdom(val sortType: KingdomSortType) : AppSortType(sortType.text)
    data class Library(val sortType: LibraryViewModel.SortType) : AppSortType(sortType.text)
}
