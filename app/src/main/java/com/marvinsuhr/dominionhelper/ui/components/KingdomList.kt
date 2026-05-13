package com.marvinsuhr.dominionhelper.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.marvinsuhr.dominionhelper.R
import com.marvinsuhr.dominionhelper.model.Card
import com.marvinsuhr.dominionhelper.model.Kingdom
import com.marvinsuhr.dominionhelper.utils.Constants
import com.marvinsuhr.dominionhelper.utils.getDrawableId
import kotlin.text.ifEmpty

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KingdomList(
    kingdomList: List<Kingdom>,
    hasOwnedExpansions: Boolean,
    onGenerateKingdom: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL),
        modifier = Modifier.fillMaxSize()
    ) {

        item {
            //GenerateKingdomButton(onGenerateKingdom)
            Spacer(Modifier) // This forces the list to stay on top
        }

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
fun GenerateKingdomButton(
    onGenerateKingdom: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(), // To center the button within the available width
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { onGenerateKingdom() },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Generate a new kingdom ")
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
            imageVector = Icons.Outlined.Casino,
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
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (hasOwnedExpansions) {
            Text(
                text = "You have expansions selected! Tap the + button to generate your first kingdom",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Customize generation rules and constraints in the Settings tab",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
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

    if (cardsToDisplay.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth(),
        onClick = {
            //if (!isEditingName) { // Allow card click only while not editing
            onKingdomClick()
            //}
        }
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                ) {
                    rowItems.forEach { (card, _) ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CardImage2(card = card)
                        }
                    }

                }
            }
        }

    }
}

@Composable
fun FavoriteButton(onFavoriteClick: () -> Unit, isFavorite: Boolean) {
    IconButton(onClick = onFavoriteClick) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = "Favorite kingdom"
        )
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
fun CardImage2(card: Card) {

    val context = LocalContext.current
    val drawableId = getDrawableId(context, card.imageName)

    Box(
        modifier = Modifier
            .padding(Constants.PADDING_SMALL)
            .clip(RoundedCornerShape(Constants.IMAGE_ROUNDED))
    ) {
        AsyncImage(
            model = drawableId,
            contentDescription = stringResource(
                id = R.string.card_image_content_description,
                card.name,
            ),
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
fun DeleteButton(onDeleteClick: () -> Unit, isEditing: Boolean, commitNameChange: () -> Unit) {

    if (isEditing) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(onClick = commitNameChange) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Done editing name"
                )
            }
        }

    } else {
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete kingdom"
            )
        }
    }

}