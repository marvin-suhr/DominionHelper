package dev.msuhr.dominionkingdoms.ui.components

import android.util.Log
import android.content.Intent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import dev.msuhr.dominionkingdoms.model.RuleOption

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.msuhr.dominionkingdoms.ui.SettingItem
import androidx.core.net.toUri
import coil.compose.AsyncImage
import dev.msuhr.dominionkingdoms.utils.Constants
import dev.msuhr.dominionkingdoms.utils.getDrawableId


@Composable
fun SettingsList(
    settings: List<SettingItem>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    showVersionInfo: Boolean = true
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
                is SettingItem.FeedbackSetting -> FeedbackSettingItem(setting)
                is SettingItem.NavigationSetting -> NavigationSettingItem(setting)
                is SettingItem.RangeRuleSetting -> RangeRuleSettingItem(setting)
            }
        }

        if (showVersionInfo) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val context = LocalContext.current
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val versionName = packageInfo.versionName
                    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    }

                    Text(
                        text = "v${versionName}_$versionCode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                }
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
            .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
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
        Column(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp),
            verticalArrangement = if (setting.description != null) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.Center
            }
        ) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (setting.description != null) {
                Text(
                    text = setting.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
            verticalAlignment = Alignment.CenterVertically
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
                    style = MaterialTheme.typography.titleLarge,
                    color = if (setting.number > setting.min) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newText ->
                    // Only allow numbers and max 2 digits
                    if (newText.all { it.isDigit() } && newText.length <= 2) {
                        newText.toIntOrNull()?.let { number ->
                            val clampedValue = number.coerceIn(setting.min, setting.max)
                            textFieldValue = clampedValue.toString()
                            setting.onNumberChange(clampedValue)
                        }
                    }
                },
                modifier = Modifier
                    .width(48.dp)
                    .height(40.dp),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        innerTextField()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 1.dp, bottom = 1.dp, start = 1.dp, top = 1.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
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
                    style = MaterialTheme.typography.titleLarge,
                    color = if (setting.number < setting.max) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}

@Composable
fun <E : Enum<E>> ChoiceSettingItem(setting: SettingItem.ChoiceSetting<E>) {
    var showDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(16.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        if (setting.imageName.isNotEmpty()) {
            val drawableId = getDrawableId(context, setting.imageName)
            AsyncImage(
                model = drawableId,
                contentDescription = "${setting.title} icon",
                modifier = Modifier
                    .padding(end = Constants.PADDING_MEDIUM)
                    .size(Constants.SETTING_ICON_SIZE),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min) // Forces the Row to follow the Text's height
            ) {
                Text(
                    text = setting.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (setting.description != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { showInfoDialog = true }
                            .padding(start = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Information",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxHeight()
                                .height(0.dp) // <--- CRITICAL: Prevents Icon from pushing Row height
                                .aspectRatio(1f)
                            //modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = setting.optionDisplayFormatter(setting.selectedOption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
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

    if (showInfoDialog && setting.description != null) {
        InfoDialog(
            title = setting.title,
            description = setting.description,
            onDismiss = { showInfoDialog = false }
        )
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
fun NavigationSettingItem(setting: SettingItem.NavigationSetting) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { setting.onClick() }
            .padding(16.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (setting.description != null) {
                Text(
                    text = setting.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeedbackSettingItem(setting: SettingItem.FeedbackSetting) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:marvin@msuhr.dev".toUri()
                    putExtra(Intent.EXTRA_SUBJECT, "Dominion Kingdoms Feedback")
                    putExtra(Intent.EXTRA_TEXT, "Hi Marvin,\n\n")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case where no email app is available
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
                .defaultMinSize(minHeight = 48.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = setting.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Send feedback",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

@Composable
fun RangeRuleSettingItem(setting: SettingItem.RangeRuleSetting) {
    var componentWidth by remember { mutableStateOf(0f) }

    // Helper to get step index from X offset
    fun getStepIndex(x: Float): Int {
        if (componentWidth <= 0) return 0
        val stepWidth = componentWidth / 4
        return (x / stepWidth).toInt().coerceIn(0, 3)
    }

    // Helper to map index to logical card count
    fun indexToCount(index: Int, isMin: Boolean): Int = when (index) {
        3 -> if (isMin) 3 else RuleOption.MAX_CARDS
        else -> index
    }

    // Convert current min/max to display indices (0-3)
    fun countToIndex(count: Int): Int = when (count) {
        RuleOption.MAX_CARDS -> 3
        3 -> 3
        else -> count.coerceAtMost(3)
    }

    // Logical current state label
    val label = when {
        setting.min == 0 && setting.max == 0 -> "Exclude"
        setting.min == 0 && setting.max == RuleOption.MAX_CARDS -> "Allow"
        setting.min == setting.max -> "Exactly ${setting.min}"
        setting.max == RuleOption.MAX_CARDS -> "At least ${setting.min}"
        setting.min == 0 -> "At most ${setting.max}"
        else -> "${setting.min} to ${setting.max}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            if (setting.imageName.isNotEmpty()) {
                val drawableId = getDrawableId(context, setting.imageName)
                AsyncImage(
                    model = drawableId,
                    contentDescription = "${setting.title} icon",
                    modifier = Modifier
                        .padding(end = Constants.PADDING_MEDIUM)
                        .size(Constants.SETTING_ICON_SIZE),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
            }

            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(0.4f)
            )

            // Segmented Range Picker
            RangePicker(
                modifier = Modifier.weight(0.6f),
                currentMinIdx = countToIndex(setting.min),
                currentMaxIdx = countToIndex(setting.max),
                onRangeChange = { newMinIdx, newMaxIdx ->
                    // Full range (0-3) means "Allow"
                    if (newMinIdx == 0 && newMaxIdx == 3) {
                        setting.onRangeChange(0, RuleOption.MAX_CARDS)
                    } else {
                        setting.onRangeChange(
                            indexToCount(newMinIdx, isMin = true),
                            indexToCount(newMaxIdx, isMin = false)
                        )
                    }
                },
                getStepIndex = ::getStepIndex,
                onWidthChange = { componentWidth = it }
            )
        }

        // State Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (label == "Allow") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun RangePicker(
    modifier: Modifier = Modifier,
    currentMinIdx: Int,
    currentMaxIdx: Int,
    onRangeChange: (Int, Int) -> Unit,
    getStepIndex: (Float) -> Int,
    onWidthChange: (Float) -> Unit
) {
    val isExclude = currentMinIdx == 0 && currentMaxIdx == 0
    val isAllow = currentMinIdx == 0 && currentMaxIdx == 3

    Row(
        modifier = modifier
            .height(40.dp)
            .onGloballyPositioned { onWidthChange(it.size.width.toFloat()) }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(currentMinIdx, currentMaxIdx) {
                detectTapGestures { offset ->
                    val tappedIndex = getStepIndex(offset.x)

                    // Determine new range based on adjacency
                    val (newMinIdx, newMaxIdx) = when {
                        // Single cell selected and tapped again - go to "Allow"
                        currentMinIdx == currentMaxIdx && tappedIndex == currentMinIdx -> {
                            0 to 3
                        }
                        isAllow -> {
                            // Currently "Allow" - tapping any cell selects that cell only
                            tappedIndex to tappedIndex
                        }
                        tappedIndex == currentMinIdx - 1 -> {
                            // Tapped adjacent to current min - extend range downward
                            tappedIndex to currentMaxIdx
                        }
                        tappedIndex == currentMaxIdx + 1 -> {
                            // Tapped adjacent to current max - extend range upward
                            currentMinIdx to tappedIndex
                        }
                        tappedIndex in currentMinIdx..currentMaxIdx -> {
                            // Tapped within current range - reset to just that cell
                            tappedIndex to tappedIndex
                        }
                        else -> {
                            // Tapped non-adjacent cell - reset to just that cell
                            tappedIndex to tappedIndex
                        }
                    }

                    onRangeChange(newMinIdx, newMaxIdx)
                }
            }
            .pointerInput(Unit) {
                var startStep = -1
                detectDragGestures(
                    onDragStart = { offset -> startStep = getStepIndex(offset.x) },
                    onDrag = { change, _ ->
                        val currentStep = getStepIndex(change.position.x)
                        if (startStep != -1) {
                            val minIdx = minOf(startStep, currentStep)
                            val maxIdx = maxOf(startStep, currentStep)
                            onRangeChange(minIdx, maxIdx)
                        }
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val steps = listOf("0", "1", "2", "3+")

        steps.forEachIndexed { index, stepLabel ->
            val isSelected = index in currentMinIdx..currentMaxIdx

            val backgroundColor = when {
                isExclude && index == 0 -> MaterialTheme.colorScheme.errorContainer
                isSelected && !isAllow -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }

            val contentColor = when {
                isExclude && index == 0 -> MaterialTheme.colorScheme.onErrorContainer
                isSelected && !isAllow -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        width = if (index > 0) 0.5.dp else 0.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun InfoDialog(
    title: String,
    description: String,
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
                // Title row with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Description text
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
