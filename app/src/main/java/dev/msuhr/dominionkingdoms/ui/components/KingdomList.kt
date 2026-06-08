package dev.msuhr.dominionkingdoms.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Castle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.Kingdom
import dev.msuhr.dominionkingdoms.utils.Constants
import dev.msuhr.dominionkingdoms.utils.getDrawableId
import kotlin.text.ifEmpty

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KingdomList(
    kingdomList: List<Kingdom>,
    hasOwnedExpansions: Boolean,
    onKingdomClicked: (Kingdom) -> Unit,
    onDeleteClick: (Kingdom) -> Unit,
    onFavoriteClick: (Kingdom) -> Unit,
    onKingdomNameChange: (kingdomUuid: String, newName: String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    paddingValues: PaddingValues
) {
    LazyColumn(
        contentPadding = paddingValues,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(Constants.PADDING_MEDIUM),
        modifier = Modifier.fillMaxSize()
    ) {

        if (kingdomList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .fillMaxWidth()
                ) {
                    EmptyKingdomsListMessage(hasOwnedExpansions)
                }
            }
        } else {
            items(
                items = kingdomList,
                key = { kingdom -> kingdom.uuid }
            ) { kingdom ->
                KingdomCard(
                    kingdom = kingdom,
                    onDeleteClick = { onDeleteClick(kingdom) },
                    onKingdomClick = { onKingdomClicked(kingdom) },
                    onFavoriteClick = { onFavoriteClick(kingdom) },
                    onKingdomNameChange = { uuid, newName -> onKingdomNameChange(uuid, newName) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun EmptyKingdomsListMessage(hasOwnedExpansions: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Castle,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Text(
            text = "No kingdoms generated yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (hasOwnedExpansions) {
            Text(
                text = "Tap the + button to generate your first kingdom.\n\nCustomize generation rules and constraints in the Settings tab",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        } else {
            Text(
                text = "Select your owned expansions in the Library tab to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun KingdomCard(
    kingdom: Kingdom,
    onDeleteClick: () -> Unit,
    onKingdomClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onKingdomNameChange: (uuid: String, newName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardsToDisplay = kingdom.randomCards.entries.take(10).toList()
    val numColumns = 5

    if (cardsToDisplay.isEmpty()) return // TODO throw error

    Card(
        modifier = modifier
            .fillMaxWidth(),
        onClick = {
            //if (!isEditingName) { // Allow card click only while not editing
            onKingdomClick()
            //}
        }
    ) {
        Column (
            verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL),
            modifier = Modifier.padding(Constants.PADDING_SMALL)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditableKingdomName(
                    kingdom,
                    onFavoriteClick,
                    onKingdomNameChange,
                    onDeleteClick = onDeleteClick,
                    modifier = Modifier.weight(1f)
                )
            }

            cardsToDisplay.chunked(numColumns).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    rowItems.forEach { (card, _) ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CardImageKingdomList(card = card)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditableKingdomName(
    kingdom: Kingdom,
    onFavoriteClick: () -> Unit,
    onNameChange: (uuid: String, newName: String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val oldName = kingdom.name
    val uuid = kingdom.uuid

    var isEditingName by remember { mutableStateOf(false) }
    var textField by remember(oldName) {
        mutableStateOf(TextFieldValue(oldName))
    }

    var displayName by remember(oldName) { mutableStateOf(oldName) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Function to commit the name change
    val commitNameChange = {
        val newName = textField.text
        if (newName.isNotBlank() && newName != oldName) {
            onNameChange(uuid, newName)
            displayName = newName
        }
        isEditingName = false
        focusManager.clearFocus()
    }

    // Handle back navigation while editing
    BackHandler(enabled = isEditingName) {
        commitNameChange()
    }

    FavoriteButton(onFavoriteClick, kingdom.isFavorite)

    // Editable Kingdom Name Area
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .height(IntrinsicSize.Min),
        contentAlignment = Alignment.Center
    ) {
        if (isEditingName) {
            BasicTextField(
                value = textField,
                onValueChange = { newValue ->
                    if (newValue.text.length <= Constants.KINGDOM_NAME_MAX_LENGTH) {
                        textField = newValue
                    }
                },
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        commitNameChange()
                    }
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && isEditingName) {
                            // If focus is lost and was editing, commit the change
                            // This check 'isEditingName' is important to avoid committing when initially focusing
                            // However, relying solely on onFocusChanged for commit can be tricky
                            // as focus can be lost for various reasons.
                            // The onKeyEvent for Enter or a dedicated Done button is often more reliable.
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                            commitNameChange()
                            true // Consume the event
                        } else {
                            false // Do not consume
                        }
                    }
            )
            // Request focus when editing starts
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                textField = TextFieldValue(
                    text = oldName,
                    selection = TextRange(0, oldName.length) // Select all
                )
            }
        } else {
            Text(
                text = displayName.ifEmpty { "Unnamed Kingdom" }, // Display current kingdom name or placeholder
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { isEditingName = true }
            )
        }
    }

    DeleteButton(onDeleteClick, isEditingName, commitNameChange)
}

@Composable
fun CardImageKingdomList(card: Card) {

    val context = LocalContext.current
    val drawableId = getDrawableId(context, card.imageName)

    // 1. Fetch the type colors list from your card entity
    val typeColors = card.getColorByTypes()

    // 2. Build a dynamic Brush depending on the count of types
    val borderBrush = remember(typeColors) {
        if (typeColors.size == 1) {
            SolidColor(typeColors.first())
        } else {
            // For multi-type cards, create a premium linear gradient running from top-left to bottom-right
            Brush.linearGradient(colors = typeColors)
        }
    }

    Box(
        modifier = Modifier
            .border(
                //border = BorderStroke(1.5.dp, borderBrush), // Colored variant. too loud
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
            )
            .clip(RoundedCornerShape(Constants.IMAGE_ROUNDED))
    ) {
        AsyncImage(
            model = drawableId,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 2.52f
                    scaleY = 2.52f

                }
                .offset {
                    IntOffset(
                        x = 0,
                        y = 31
                    )
                }
        )
    }
}

@Composable
fun FavoriteButton(onFavoriteClick: () -> Unit, isFavorite: Boolean) {
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
        contentDescription = "Favorite kingdom",
        modifier = Modifier
            .clip(CircleShape) // Caps the touch ripple to a clean circle
            .clickable { onFavoriteClick() }
            .padding(4.dp)
    )
}

@Composable
fun DeleteButton(onDeleteClick: () -> Unit, isEditing: Boolean, commitNameChange: () -> Unit) {
    if (isEditing) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Done editing name",
            modifier = Modifier
                .clip(CircleShape)
                .clickable { commitNameChange() }
                .padding(4.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "Delete kingdom",
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onDeleteClick() }
                .padding(4.dp)
        )
    }
}