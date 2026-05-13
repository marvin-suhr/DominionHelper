package com.marvinsuhr.dominionhelper.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marvinsuhr.dominionhelper.ui.SettingItem


@Composable
fun SettingsList(
    settings: List<SettingItem>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {

    Log.i("SettingsList", "settings: $settings")

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = paddingValues
    ) {
        items(settings) { setting ->
            when (setting) {
                is SettingItem.SectionHeader -> SectionHeaderItem(setting)
                is SettingItem.SwitchSetting -> SwitchSettingItem(setting)
                is SettingItem.TextSetting -> TextSettingItem(setting)
                is SettingItem.NumberSetting -> NumberSettingItem(setting)
                is SettingItem.ChoiceSetting<*> -> ChoiceSettingItem(setting)
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeaderItem(setting: SettingItem.SectionHeader) {
    Text(
        text = setting.title,
        modifier = Modifier
            .fillMaxWidth()
            // Asymmetric padding: larger top gap, smaller bottom gap
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SwitchSettingItem(setting: SettingItem.SwitchSetting) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setting.title,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = setting.isChecked,
            onCheckedChange = setting.onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSettingItem(setting: SettingItem.TextSetting) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = setting.title)
        TextField(
            value = setting.text,
            onValueChange = setting.onTextChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberSettingItem(setting: SettingItem.NumberSetting) {

    var textFieldValue by remember(setting.number) { mutableStateOf(setting.number.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setting.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Minus button
            IconButton(
                onClick = {
                    val newValue = (setting.number - 1).coerceAtLeast(setting.min)
                    textFieldValue = newValue.toString()
                    setting.onNumberChange(newValue)
                },
                enabled = setting.number > setting.min
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newText ->
                    textFieldValue = newText
                    newText.toIntOrNull()?.let { number ->
                        val clampedValue = number.coerceIn(setting.min, setting.max)
                        setting.onNumberChange(clampedValue)
                    }
                },
                modifier = Modifier.width(64.dp),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                ),
                singleLine = true,
                readOnly = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Plus button
            IconButton(
                onClick = {
                    val newValue = (setting.number + 1).coerceAtMost(setting.max)
                    textFieldValue = newValue.toString()
                    setting.onNumberChange(newValue)
                },
                enabled = setting.number < setting.max
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun <E : Enum<E>> ChoiceSettingItem(setting: SettingItem.ChoiceSetting<E>) {
    var showDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = setting.optionDisplayFormatter(setting.selectedOption),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showDialog) {
            EnumSelectionDialog(
                title = setting.title,
                options = setting.allOptions,
                selectedOption = setting.selectedOption,
                optionDisplayFormatter = setting.optionDisplayFormatter,
                onOptionSelected = { option ->
                    setting.onOptionSelected(option)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <E : Enum<E>> EnumSelectionDialog(
    title: String,
    options: List<E>,
    selectedOption: E,
    optionDisplayFormatter: (E) -> String,
    onOptionSelected: (E) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .widthIn(max = 400.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onOptionSelected(option) }
                        )
                        Text(
                            text = optionDisplayFormatter(option),
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    Icon(
        imageVector = if (selected) {
            Icons.Filled.Circle
        } else {
            Icons.Outlined.Circle
        },
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    )
}