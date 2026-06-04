package dev.msuhr.dominionkingdoms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.msuhr.dominionkingdoms.CurrentScreen
import dev.msuhr.dominionkingdoms.model.AppSortType
import dev.msuhr.dominionkingdoms.ui.KingdomViewModel
import dev.msuhr.dominionkingdoms.ui.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    showBackButton: Boolean,
    onBackButtonClicked: () -> Unit,
    currentScreen: CurrentScreen,
    onSortTypeSelected: (AppSortType) -> Unit,
    selectedSortType: AppSortType?,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showGridViewToggle: Boolean = false,
    isGridViewEnabled: Boolean = false,
    onGridViewToggle: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                modifier = Modifier.sizeIn(maxWidth = 300.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = { onBackButtonClicked() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }

            // Grid view toggle icon (only shown when viewing a kingdom)
            if (showGridViewToggle) {
                IconButton(onClick = onGridViewToggle) {
                    Icon(
                        if (isGridViewEnabled) Icons.Outlined.ViewModule else Icons.Filled.ViewList,
                        contentDescription = if (isGridViewEnabled) "Show list view" else "Show grid view"
                    )
                }
            }

            if (currentScreen != CurrentScreen.Settings) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Localized description"
                    )
                }
            }

            SortDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                selectedSortType = selectedSortType,
                onSortTypeSelected = onSortTypeSelected,
                currentScreen = currentScreen
            )

        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun SortDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedSortType: AppSortType?,
    onSortTypeSelected: (AppSortType) -> Unit,
    currentScreen: CurrentScreen
) {

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        if  (selectedSortType != null) {

            if (currentScreen == CurrentScreen.Library) {
                LibraryViewModel.SortType.entries.forEach { sortOption ->
                    val appSortType = AppSortType.Library(sortOption)
                    SortDropdownMenuItem(
                        sortType = appSortType,
                        selectedSortType = selectedSortType,
                        onSortTypeSelected = onSortTypeSelected,
                        onDismissRequest = onDismissRequest
                    )
                }
            } else if (currentScreen == CurrentScreen.Kingdoms) {
                KingdomViewModel.SortType.entries.forEach { sortOption ->
                    val appSortType = AppSortType.Kingdom(sortOption)
                    SortDropdownMenuItem(
                        sortType = appSortType,
                        selectedSortType = selectedSortType,
                        onSortTypeSelected = onSortTypeSelected,
                        onDismissRequest = onDismissRequest
                    )
                }
            }
        }
    }
}

@Composable
fun SortDropdownMenuItem(
    sortType: AppSortType,
    selectedSortType: AppSortType,
    onSortTypeSelected: (AppSortType) -> Unit,
    onDismissRequest: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(sortType.text) },
        onClick = {
            onSortTypeSelected(sortType)
            onDismissRequest()
        },
        modifier = Modifier.background(
            if (sortType == selectedSortType) MaterialTheme.colorScheme.primaryContainer
            else Color.Transparent
        ),
        trailingIcon = {
            if (selectedSortType == sortType) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "$sortType selected"
                )
            }
        }
    )
}