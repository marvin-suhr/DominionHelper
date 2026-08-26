package dev.msuhr.dominionkingdoms.ui.components

import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import dev.msuhr.dominionkingdoms.utils.getDrawableId
import dev.msuhr.dominionkingdoms.R
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.CardDisplayCategory
import dev.msuhr.dominionkingdoms.model.Kingdom
import dev.msuhr.dominionkingdoms.model.OwnedEdition
import dev.msuhr.dominionkingdoms.model.Type
import dev.msuhr.dominionkingdoms.ui.LibraryViewModel
import dev.msuhr.dominionkingdoms.utils.Constants
import dev.msuhr.dominionkingdoms.model.Set
import dev.msuhr.dominionkingdoms.utils.ui.horizontalFadingEdges
import dev.msuhr.dominionkingdoms.utils.ui.swipeLockGatekeeper
import kotlin.math.cos
import kotlin.math.sin

class SwipeLock {
    var activeCardId: Int? = null
}

// TODO: Check Box contentAlignment vs contents Modifier.align (first is better)
// Displays a list of cards
@Composable
fun LibraryCardList(
    modifier: Modifier = Modifier,
    cardList: List<Card>,
    sortType: LibraryViewModel.SortType,
    includeEditionSelection: Boolean = false,
    selectedEdition: OwnedEdition,
    onEditionSelected: (Int, OwnedEdition) -> Unit,
    onCardClick: (Card) -> Unit,
    onToggleEnable: (Card) -> Unit = { },
    onFavorite: (Card) -> Unit = { },
    onBan: (Card) -> Unit = { },
    listState: LazyListState = rememberLazyListState(),
    paddingValues: PaddingValues,
    onSortTypeSelected: (LibraryViewModel.SortType) -> Unit = {}
) {
    var showSortDialog by remember { mutableStateOf(false) }

    val supplyCards = remember(cardList) {
        cardList.filter { it.getDisplayCategory() == CardDisplayCategory.SUPPLY }
    }

    val specialCards = remember(cardList) {
        cardList.filter { it.getDisplayCategory() == CardDisplayCategory.SPECIAL }
    }

    val landscapeCards = remember(cardList) {
        cardList.filter { it.getDisplayCategory() == CardDisplayCategory.LANDSCAPE }
    }

    val materialCards = remember(cardList) {
        cardList.filter { it.getDisplayCategory() == CardDisplayCategory.MATERIAL }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL)
    ) {
        if (includeEditionSelection) {
            item {
                EditionSelectionButtons(onEditionSelected, selectedEdition)
            }
        }

        // Skip card spacers when not sorting by card type
        if (sortType != LibraryViewModel.SortType.TYPE) {
            items(cardList) { card ->
                CardView(
                    card,
                    onCardClick = onCardClick,
                    enabled = card.isEnabled,
                    showIcon = false,
                    onToggleEnable = { onToggleEnable(card) },
                    onFavorite = { onFavorite(card) },
                    onBan = { onBan(card) }
                )
            }
        } else {

            // Kingdom cards in library
            if (supplyCards.isNotEmpty()) {
                item {
                    CardSpacer("Kingdom Cards (${supplyCards.size})")
                }
                items(supplyCards, key = { card -> "supply_${card.id}" }) { card ->
                    CardView(
                        card = card,
                        onCardClick = { onCardClick(card) }, // Pass the clicked card
                        enabled = card.isEnabled,
                        showIcon = false,
                        onToggleEnable = { onToggleEnable(card) },
                        onFavorite = { onFavorite(card) },
                        onBan = { onBan(card) }
                    )
                }
            }

            // Other cards in library
            if (specialCards.isNotEmpty()) {
                item {
                    CardSpacer("Other Cards (${specialCards.size})")
                }
                items(
                    specialCards, key = { card -> "special_supply_${card.id}" }) { card ->
                    CardView(
                        card = card,
                        onCardClick = { onCardClick(card) },
                        enabled = card.isEnabled,
                        showIcon = false,
                        onToggleEnable = { onToggleEnable(card) },
                        onFavorite = { onFavorite(card) },
                        onBan = { onBan(card) }
                    )
                }
            }

            // Landscape cards in library
            if (landscapeCards.isNotEmpty()) {
                item {
                    CardSpacer("Landscape Cards (${landscapeCards.size})")
                }
                items(landscapeCards, key = { card -> "landscape_${card.id}" }) { card ->
                    CardView(
                        card = card,
                        onCardClick = { onCardClick(card) },
                        enabled = card.isEnabled,
                        showIcon = false,
                        onToggleEnable = { onToggleEnable(card) },
                        onFavorite = { onFavorite(card) },
                        onBan = { onBan(card) }
                    )
                }
            }

            // Additional Material (TOKEN/MAT cards) in library
            if (materialCards.isNotEmpty()) {
                item {
                    CardSpacer("Additional Material (${materialCards.size})")
                }
                items(materialCards, key = { card -> "material_${card.id}" }) { card ->
                    CardView(
                        card = card,
                        onCardClick = { onCardClick(card) },
                        enabled = card.isEnabled,
                        showIcon = false,
                        onToggleEnable = { onToggleEnable(card) },
                        onFavorite = { onFavorite(card) },
                        onBan = { onBan(card) }
                    )
                }
            }
        }
    }

    if (showSortDialog) {
        SortTypeDialog(
            sortType = sortType,
            onSortTypeSelected = {
                onSortTypeSelected(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

@Composable
fun EditionSelectionButtons(
    onEditionSelected: (Int, OwnedEdition) -> Unit,
    selectedEdition: OwnedEdition = OwnedEdition.BOTH
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { onEditionSelected(1, selectedEdition) },
            colors = if (selectedEdition == OwnedEdition.FIRST || selectedEdition == OwnedEdition.BOTH) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        ) {
            Text("1st Edition")
        }
        Button(
            onClick = { onEditionSelected(2, selectedEdition) },
            colors = if (selectedEdition == OwnedEdition.SECOND || selectedEdition == OwnedEdition.BOTH) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        ) {
            Text("2nd Edition")
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun KingdomCardList(
    modifier: Modifier = Modifier,
    kingdom: Kingdom,
    onCardClick: (Card) -> Unit,
    selectedPlayers: Int,
    onPlayerCountChange: (Int) -> Unit,
    listState: LazyGridState = rememberLazyGridState(),
    isCardDismissEnabled: Boolean,
    isLandscapeDismissEnabled: Boolean,
    onCardDismissed: (Card) -> Unit,
    paddingValues: PaddingValues,
    isGridViewEnabled: Boolean = false
) {
    Log.i(
        "KingdomList",
        "randomCards: ${kingdom.randomCards.size}, basicCards: ${kingdom.basicCards.size}, dependentCards: ${kingdom.dependentCards.size}, startingCards: ${kingdom.startingCards.size}, landscapeCards: ${kingdom.landscapeCards.size}, gridView: $isGridViewEnabled"
    )

    val swipeLock = remember { SwipeLock() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        state = listState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL),
        horizontalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL)
    ) {

        item(span = { GridItemSpan(2) }) {
            PlayerSelectionButtons(
                selectedPlayers = selectedPlayers,
                onPlayerSelected = { onPlayerCountChange(it) }
            )
        }

        // RANDOM CARDS
        item(span = { GridItemSpan(2) }) {
            CardSpacer("Supply Cards" + if (kingdom.randomCards.size > 10) " (${kingdom.randomCards.size} / 10)" else "")
        }

        // TODO REVIEW
        itemsIndexed(
            items = kingdom.randomCards.keys.toList(),
            key = { _, card -> "random_${card.id}" },
            span = { _, _ -> GridItemSpan(if (isGridViewEnabled) 1 else 2) }
        ) { index, card ->
            val isLeftColumn = index % 2 == 0

            if (isCardDismissEnabled) {
                DismissableCard(
                    card = card,
                    amount = kingdom.randomCards[card]!!,
                    onCardDismissed = onCardDismissed,
                    onCardClick = onCardClick,
                    modifier = Modifier.animateItem(),
                    isEnabled = card.isEnabled,
                    useGridView = isGridViewEnabled,
                    enableDismissFromStartToEnd = !isGridViewEnabled || !isLeftColumn,
                    enableDismissFromEndToStart = !isGridViewEnabled || isLeftColumn,
                    swipeLock = swipeLock
                )
            } else {
                if (isGridViewEnabled) {
                    KingdomGridCardItem(
                        card = card,
                        amount = kingdom.randomCards[card]!!,
                        onCardClick = onCardClick,
                        modifier = Modifier.animateItem()
                    )
                } else {
                    CardView(
                        card,
                        amount = kingdom.randomCards[card]!!,
                        onCardClick,
                        enabled = card.isEnabled,
                        showIcon = true,
                        isContextMenuEnabled = false,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // LANDSCAPE CARDS
        if (kingdom.hasLandscapeCards()) {
            item(span = { GridItemSpan(2) }) {
                CardSpacer("Landscape Cards")
            }
            items(
                items = kingdom.landscapeCards.keys.toList(),
                key = { card -> "landscape_${card.id}" },
                span = { GridItemSpan(2) }
            ) { card ->
                if (isLandscapeDismissEnabled) {
                    DismissableCard(
                        card = card,
                        onCardDismissed = onCardDismissed,
                        onCardClick = onCardClick,
                        modifier = Modifier.animateItem(),
                        isEnabled = card.isEnabled,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                        swipeLock = swipeLock
                    )
                } else {
                    CardView(
                        card,
                        amount = kingdom.landscapeCards[card]!!,
                        onCardClick = onCardClick,
                        enabled = card.isEnabled,
                        showIcon = true,
                        isContextMenuEnabled = false,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // DEPENDENT CARDS
        if (kingdom.hasDependentCards()) {
            item(span = { GridItemSpan(2) }) {
                CardSpacer("Additional Material")
            }
            items(kingdom.dependentCards.keys.toList(), key = { "dependent_${it.id}" }, span = { GridItemSpan(2) }) { card ->
                CardView(
                    card,
                    amount = kingdom.dependentCards[card]!!,
                    onCardClick = onCardClick,
                    enabled = card.isEnabled,
                    showIcon = true,
                    isContextMenuEnabled = false,
                    modifier = Modifier.animateItem() // I think that's only needed for dismissable cards?
                )
            }
        }

        // STARTING CARDS
        item(span = { GridItemSpan(2) }) {
            CardSpacer("Starting Cards")
        }
        items(kingdom.startingCards.keys.toList(), key = { "starting_${it.id}" }, span = { GridItemSpan(2) }) { card ->
            CardView(
                card,
                amount = kingdom.startingCards[card]!!,
                onCardClick = onCardClick,
                enabled = card.isEnabled,
                showIcon = true,
                isContextMenuEnabled = false,
                modifier = Modifier.animateItem()
            )
        }

        // BASIC CARDS
        item(span = { GridItemSpan(2) }) {
            CardSpacer("Basic Cards")
        }
        items(kingdom.basicCards.keys.toList(), key = { "basic_${it.id}" }, span = { GridItemSpan(2) }) { card ->
            CardView(card, amount = kingdom.basicCards[card]!!, onCardClick = onCardClick, enabled = card.isEnabled, showIcon = true, isContextMenuEnabled = false, modifier = Modifier.animateItem())
        }
    }
}

@Composable
fun PlayerSelectionButtons(selectedPlayers: Int, onPlayerSelected: (Int) -> Unit) {
    val playerCounts = listOf(2, 3, 4, 5, 6)

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp) // Button height
            .horizontalFadingEdges(fadeWidth = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // This the only way I can get it to look the way I want
        item { Spacer(modifier = Modifier.width(0.dp)) }
        items(playerCounts) { count ->
            Button(
                onClick = { onPlayerSelected(count) },
                colors = if (selectedPlayers == count) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            ) {
                Text("$count Players")
            }
        }
        item { Spacer(modifier = Modifier.width(0.dp)) }
    }
}

@Composable
fun KingdomGridCardItem(
    card: Card,
    amount: Int = 1,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {

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

    Card(
        onClick = { onCardClick(card) },
        modifier = modifier
            .border(border = BorderStroke(3.dp, borderBrush), shape = RoundedCornerShape(Constants.IMAGE_ROUNDED))
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
            ),
        shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.PADDING_SMALL),
            horizontalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL),
            verticalAlignment = Alignment.CenterVertically // Centers both columns
        ) {
            // Left Column: Expansion Icon + Price Badge
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .align(Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL)
            ) {

                // Expansion icon
                Image(
                    painter = painterResource(id = card.expansionImageId),
                    contentDescription = card.sets.first().displayName,
                    modifier = Modifier.size(30.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )

                Spacer(Modifier.height(Constants.PADDING_SMALL))

                Row (horizontalArrangement = Arrangement.Center) {
                    // Price Indicator
                    if (card.cost != null) {
                        NumberCircle(card.cost.toString())
                    }
                    if (card.debt != null) {
                        NumberHexagon(card.debt)
                    }
                    if (card.potion) {
                        PotionIcon()
                    }
                }
            }

            // Card image with text on it
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.3f)
                        .clip(RoundedCornerShape(Constants.IMAGE_ROUNDED))
                        .border(
                            border = BorderStroke(
                                2.5.dp,
                                MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
                        )
                ) {
                    AsyncImage(
                        model = getDrawableId(LocalContext.current, card.imageName),
                        contentDescription = stringResource(
                            id = R.string.card_image_content_description,
                            card.name
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 2.5f
                                scaleY = 2.5f
                            }
                            .offset {
                                IntOffset(x = 0, y = 45)
                            }
                    )

                    // Card name
                    Text(
                        text = card.name + if (amount > 1) " ($amount)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(vertical = Constants.PADDING_MINI)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissableCard(
    card: Card,
    amount: Int = 1,
    onCardDismissed: (Card) -> Unit,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    useGridView: Boolean = false,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    swipeLock: SwipeLock
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    val currentEnableStartToEnd by rememberUpdatedState(enableDismissFromStartToEnd)
    val currentEnableEndToStart by rememberUpdatedState(enableDismissFromEndToStart)

    val dismissState = remember(card.id) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            density = density,
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.Settled) {
                    true
                } else {
                    // Always checks the updated tracking reference pointers
                    when (dismissValue) {
                        SwipeToDismissBoxValue.StartToEnd -> currentEnableStartToEnd
                        SwipeToDismissBoxValue.EndToStart -> currentEnableEndToStart
                    }
                }
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.25f }
        )
    }

    // --- SIDE EFFECTS & LOCK LIFECYCLE MANAGEMENT ---

    // Lock Lifecycle 1: Release lock instantly if the card springs back to the center
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled && swipeLock.activeCardId == card.id) {
            swipeLock.activeCardId = null
        }
    }

    // Lock Lifecycle 2: Clear lock and run state updates when card slides entirely off-screen
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            if (swipeLock.activeCardId == card.id) swipeLock.activeCardId = null
            onCardDismissed(card)
        }
    }

    // Lock Lifecycle 3: Absolute insurance policy to clear lock slots if layout node unmounts mid-drag
    DisposableEffect(card.id) {
        onDispose {
            if (swipeLock.activeCardId == card.id) {
                swipeLock.activeCardId = null
            }
        }
    }

    // --- RENDER LAYER ---

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.swipeLockGatekeeper(card.id, swipeLock, dismissState),
        enableDismissFromStartToEnd = enableDismissFromStartToEnd,
        enableDismissFromEndToStart = enableDismissFromEndToStart,
        backgroundContent = {
            DismissBackgroundCanvas(
                dismissState = dismissState,
                enableStart = enableDismissFromStartToEnd,
                enableEnd = enableDismissFromEndToStart
            )
        }
    ) {
        if (useGridView) {
            KingdomGridCardItem(card, amount, onCardClick)
        } else {
            CardView(card, amount, onCardClick, isEnabled, isContextMenuEnabled = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackgroundCanvas(
    dismissState: SwipeToDismissBoxState,
    enableStart: Boolean,
    enableEnd: Boolean
) {
    val direction = try { dismissState.dismissDirection } catch (_: IllegalStateException) { null }
    val isSwipingFromStart = direction == SwipeToDismissBoxValue.StartToEnd && enableStart
    val isSwipingFromEnd = direction == SwipeToDismissBoxValue.EndToStart && enableEnd
    val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled

    val scale by animateFloatAsState(
        targetValue = if (isSwiping) 1.4f else 1.0f,
        label = "icon scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (isSwipingFromStart) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Dismiss Left",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
        if (isSwipingFromEnd) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Dismiss Right",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
    }
}

@Composable
fun CardSpacer(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Constants.PADDING_SMALL))
        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.5f))
        Spacer(modifier = Modifier.height(Constants.PADDING_MEDIUM))
        Text(text = text, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(Constants.PADDING_MEDIUM))
        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.5f))
        Spacer(modifier = Modifier.height(Constants.PADDING_SMALL))
    }
}

// Displays a single card, with an image and a name
// TODO parameter order
@Composable
fun CardView(
    card: Card,
    amount: Int = 1,
    onCardClick: (Card) -> Unit,
    enabled: Boolean = true,
    showIcon: Boolean = true,
    onToggleEnable: () -> Unit = { },
    onFavorite: () -> Unit = { },
    onBan: () -> Unit = { },
    isContextMenuEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showPopupMenu by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) } // Store touch coordinates
    val view = LocalView.current

    Box(
        modifier = modifier
            .height(Constants.CARD_HEIGHT_CARDS)
            .shadow(
                elevation = if (enabled) 4.dp else 0.dp,
                shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
            )
            .fillMaxWidth()
            .clip(RoundedCornerShape(Constants.IMAGE_ROUNDED))
            .alpha(if (enabled) 1f else 0.6f)
            .indication(interactionSource, LocalIndication.current) // Ripple
            // Use pointerInput to distinguish between Tap and LongPress
            .pointerInput(card, onCardClick) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        tryAwaitRelease()
                        interactionSource.emit(PressInteraction.Release(press))
                    },
                    onTap = {
                        onCardClick(card)
                    },
                    onLongPress = { offset ->
                        touchOffset = offset
                        if (!card.types.contains(Type.TOKEN) && !card.types.contains(Type.MAT)) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            showPopupMenu = true
                        }
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
        ) {
            Row {
                ColoredBar(card.getColorByTypes())
                CardImage(card)
                CardLabels(card, amount, modifier = Modifier.weight(1f))

                if (showIcon) {
                    CardIcon(card.expansionImageId, card.sets[0].name)
                } else if (card.sets.any { it == Set.PROMO } && card.supply) {
                    PromoToggle(card, onToggleEnable)
                } else if (!card.isEnabled) {
                    CardButton(onToggleEnable)
                }
            }
        }

        // The "Actual" Context Menu element
        if (isContextMenuEnabled) {
            CardContextMenu(
                expanded = showPopupMenu,
                offset = touchOffset,
                onDismiss = { showPopupMenu = false },
                onFavorite = onFavorite,
                onBan = onBan,
                isFavorite = card.isFavorite,
                isEnabled = card.isEnabled,
                hasSupply = card.supply && !card.basic && !card.types.contains(Type.PILE)
            )
        }
    }
}

@Composable
fun CardContextMenu(
    expanded: Boolean,
    offset: Offset,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onBan: () -> Unit,
    isFavorite: Boolean = false,
    isEnabled: Boolean = true,
    hasSupply: Boolean = true
) {
    val density = LocalDensity.current

    // We convert the touch offset to Dp so the menu knows where to go
    val xOffset = with(density) { offset.x.toDp() }
    val yOffset = with(density) { offset.y.toDp() }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        // offset centers the menu on the touch point
        offset = DpOffset(xOffset, yOffset - Constants.CARD_HEIGHT_CARDS)
    ) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Unfavorite Card" else "Favorite Card") },
            onClick = {
                onFavorite()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text(if (isEnabled) "Ban Card" else "Unban Card") },
            onClick = {
                onBan()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
            enabled = hasSupply
        )
    }
}

@Composable
fun ColoredBar(barColors: List<Color>) {
    if (barColors.size > 2) {
        Log.w("ColoredBar", "barColors list must contain at most two colors.")
        barColors.dropLast(barColors.size - 2)
    }

    val color1 = barColors.firstOrNull() ?: Color.Transparent
    val color2 = barColors.getOrNull(1) ?: color1

    val animatedColor1 by animateColorAsState(
        targetValue = color1,
        animationSpec = tween(durationMillis = 1000), label = "color1"
    )

    val animatedColor2 by animateColorAsState(
        targetValue = color2,
        animationSpec = tween(durationMillis = 1000), label = "color2"
    )

    val brush = if (barColors.size == 1) {
        Brush.verticalGradient(listOf(animatedColor1, animatedColor1))
    } else {
        Brush.verticalGradient(listOf(animatedColor1, animatedColor2))
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(Constants.COLOR_BAR_WIDTH)
            .background(brush)
    )
}

@Composable
fun CardImage(card: Card) {

    val context = LocalContext.current
    val drawableId = getDrawableId(context, card.imageName)

    Box(
        modifier = Modifier
            .padding(Constants.PADDING_SMALL)
            .clip(RoundedCornerShape(Constants.IMAGE_ROUNDED))
            .width(Constants.CARD_IMAGE_WIDTH)
    ) {
        AsyncImage(
            model = drawableId,
            contentDescription = stringResource(
                id = R.string.card_image_content_description,
                card.name,
            ),
            modifier = Modifier
                .fillMaxSize()
                // TODO - Too much logic - Put this elsewhere
                .graphicsLayer {
                    if (card.types.contains(Type.PILE)) {
                        scaleX = 1.25f
                        scaleY = 1.25f
                    } else if (card.types.contains(Type.MAT)) {
                        if (card.name == "Tavern Mat" || card.name == "Villagers Mat") {
                            scaleX = 1.1f
                            scaleY = 1.1f
                        } else if (card.name == "Coffers Mat" || card.name == "Favors Mat") {
                            scaleX = 1.2f
                            scaleY = 1.2f
                        } else if (card.name in listOf("Island Mat", "Pirate Ship Mat", "Native Village Mat")) {
                            scaleX = 2.05f
                            scaleY = 2.05f
                        } else {
                            scaleX = 1.4f
                            scaleY = 1.4f
                        }
                    } else if (card.types.contains(Type.TOKEN)) {
                        scaleX = 1.9f
                        scaleY = 1.9f
                    }

                    else {
                        scaleX = if (card.landscape) 2.1f else 2.5f
                        scaleY = if (card.landscape) 2.1f else 2.5f
                    }
                }
                .offset {
                    IntOffset(
                        x = 0,
                        y = when {
                            card.name == "Native Village Mat" -> -40
                            card.name in listOf("Island Mat", "Trade Route Mat") -> -15
                            card.name == "Potion" || card.types.contains(Type.TOKEN) || card.types.contains(Type.PILE) || card.types.contains(Type.MAT) -> 0
                            card.landscape || card.name == "Curse" -> 13
                            card.basic
                                    && !card.types.contains(Type.RUINS)
                                    && !card.types.contains(Type.SHELTER)
                                    && !card.types.contains(Type.HEIRLOOM) -> 26
                            else -> 31
                        }
                    )
                }
        )
    }
}

@Composable
fun CardLabels(card: Card, amount: Int, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = Constants.PADDING_MINI, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.name + if (amount > 1) " ($amount)" else "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = Constants.CARD_NAME_FONT_SIZE,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (card.isFavorite) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row {
            var previousElementExists = false

            @Composable
            fun ConditionalSpacer(width: Dp) {
                if (previousElementExists) {
                    Spacer(modifier = Modifier.width(width)) // Use width for horizontal spacing
                }
            }

            // Cost
            if (card.cost != null) {
                val modifier = if (card.overpay) "+" else if (card.specialCost) "*" else ""
                NumberCircle(card.cost.toString() + modifier)
                previousElementExists = true
            }

            // Debt
            if (card.debt != null) {
                ConditionalSpacer(Constants.PADDING_MINI)
                NumberHexagon(card.debt)
                previousElementExists = true
            }

            // Potion cost
            if (card.potion) {
                ConditionalSpacer(Constants.PADDING_MINI)
                PotionIcon()
                previousElementExists = true
            }

            // Special card types
            ConditionalSpacer(Constants.PADDING_SMALL)
            val text: String = card.types.mapNotNull { it.displayText }.joinToString(", ")

            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = Constants.TEXT_SMALL,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = Italic
            )
        }
    }
}

// Display a number in a circle (Used for card costs)
@Composable
fun NumberCircle(number: String) {
    val circleColor = Color(0xFFE5C158)
    val textColor = Color.Black.toArgb()

    Box(
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(Constants.CARD_PRICE_SIZE)
        ) {
            drawCircle(
                color = circleColor,
                radius = size.minDimension / 2,
                center = Offset(size.width / 2, size.height / 2)
            )

            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = textColor
                    textAlign = Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                    isFakeBoldText = true
                }

                val textBounds = Rect()
                paint.getTextBounds(number, 0, number.length, textBounds)

                canvas.nativeCanvas.drawText(
                    number,
                    size.width / 2,
                    (size.height / 2) - (textBounds.top + textBounds.bottom) / 2,
                    paint
                )
            }
        }
    }
}

// Display a number in a hexagon (Used for card debt)
@Composable
fun NumberHexagon(number: Int) {
    val hexagonColor = Color(0xFF965F33)
    val textColor = Color.White.toArgb()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier // Apply the passed-in modifier here
            .offset(y = (-1).dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(Constants.CARD_DEBT_SIZE)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2

            // Draw the hexagon
            drawIntoCanvas { canvas ->
                val hexagonPath = Path()
                val angle = 2.0 * Math.PI / 6 // 6 sides

                // Start at the first vertex
                hexagonPath.moveTo(
                    centerX + radius * cos(0.0).toFloat(),
                    centerY + radius * sin(0.0).toFloat()
                )

                // Draw lines to each subsequent vertex
                for (i in 1..6) {
                    hexagonPath.lineTo(
                        centerX + radius * cos(angle * i).toFloat(),
                        centerY + radius * sin(angle * i).toFloat()
                    )
                }

                // Close the path
                hexagonPath.close()
                val paint = Paint()
                paint.color = hexagonColor.toArgb()
                paint.style = Paint.Style.FILL
                canvas.nativeCanvas.drawPath(hexagonPath, paint)

                // Draw the text
                val textPaint = Paint().apply {
                    color = textColor
                    textAlign = Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                    isFakeBoldText = true
                }

                val textBounds = Rect()
                textPaint.getTextBounds(
                    number.toString(),
                    0,
                    number.toString().length,
                    textBounds
                )

                canvas.nativeCanvas.drawText(
                    number.toString(),
                    centerX - 1f,
                    centerY - (textBounds.top + textBounds.bottom) / 2,
                    textPaint
                )
            }
        }
    }
}

@Composable
fun PotionIcon() {
    AsyncImage(
        model = R.drawable.set_alchemy,
        contentDescription = "Potion icon",
        colorFilter = ColorFilter.tint(Color(0xFF3B8CD6)),
        modifier = Modifier
            .size(22.dp)
            .offset(y = 1.dp)
    )
}

@Composable
fun CardIcon(imageId: Int, setName: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
    ) {
        if (imageId != 0) {
            AsyncImage(
                model = imageId,
                contentDescription = "$setName icon",
                modifier = Modifier
                    .size(Constants.ICON_SIZE),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

// Specific toggle for Promo cards (individual ownership)
@Composable
fun PromoToggle(card: Card, onToggleOwned: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = if (card.isEnabled) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (card.isEnabled) "Owned" else "Unowned",
            modifier = Modifier.size(35.dp).clip(CircleShape).clickable { onToggleOwned() },
            tint = if (card.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CardButton(onToggleEnable: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Unban card",
            modifier = Modifier.size(Constants.ICON_SIZE).clip(CircleShape).clickable { onToggleEnable() },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun SortTypeDialog(
    sortType: LibraryViewModel.SortType,
    onSortTypeSelected: (LibraryViewModel.SortType) -> Unit,
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
                    text = "Sort by",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LibraryViewModel.SortType.entries.forEach { sortOption ->
                    val isSelected = sortOption == sortType
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSortTypeSelected(sortOption)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSortTypeSelected(sortOption) }
                        )
                        Text(
                            text = sortOption.text,
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
private fun RadioButton( // TODO huh where is this
    selected: Boolean,
    onClick: () -> Unit
) {
    Icon(
        imageVector = if (selected) {
            Icons.Filled.CheckCircle
        } else {
            Icons.Outlined.Circle
        },
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(8.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    )
}
