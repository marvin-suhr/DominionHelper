package com.marvinsuhr.dominionhelper.ui.components

import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DisabledVisible
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.marvinsuhr.dominionhelper.R
import com.marvinsuhr.dominionhelper.model.Card
import com.marvinsuhr.dominionhelper.model.CardDisplayCategory
import com.marvinsuhr.dominionhelper.model.Kingdom
import com.marvinsuhr.dominionhelper.model.OwnedEdition
import com.marvinsuhr.dominionhelper.model.Set
import com.marvinsuhr.dominionhelper.model.Type
import com.marvinsuhr.dominionhelper.ui.LibraryViewModel
import com.marvinsuhr.dominionhelper.utils.Constants
import com.marvinsuhr.dominionhelper.utils.getDrawableId
import kotlin.math.cos
import kotlin.math.sin

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
    onToggleEnable: (Card) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    paddingValues: PaddingValues
) {
    Log.i("CardList", "${cardList.size} cards")

    val supplyCards = remember(cardList) {
        cardList.filter { it.getDisplayCategory() == CardDisplayCategory.SUPPLY }
    }

    val specialCards = remember(cardList) {
        cardList.filter { it.getDisplayCategory() == CardDisplayCategory.SPECIAL }
    }

    val landscapeCards = remember(cardList) {
        cardList.filter { it.getDisplayCategory() == CardDisplayCategory.LANDSCAPE }
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

        if (sortType != LibraryViewModel.SortType.TYPE) {
            items(cardList) { card ->
                CardView(
                    card,
                    onCardClick,
                    card.isEnabled,
                    showIcon = false,
                    onToggleEnable = { onToggleEnable(card) })
            }
        } else {

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
                        onToggleEnable = { onToggleEnable(card) }
                    )
                }
            }

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
                        onToggleEnable = { onToggleEnable(card) }
                    )
                }
            }

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
                        onToggleEnable = { onToggleEnable(card) }
                    )
                }
            }
        }
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

// Displays a list of cards
@Composable
fun SearchResultsCardList(
    modifier: Modifier = Modifier,
    cardList: List<Card>,
    onCardClick: (Card) -> Unit,
    onToggleEnable: (Card) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    paddingValues: PaddingValues,
    sortType: LibraryViewModel.SortType = LibraryViewModel.SortType.TYPE,
    onSortTypeSelected: (LibraryViewModel.SortType) -> Unit = {}
) {
    Log.i("CardList", "${cardList.size} cards")

    var showSortDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL),
        state = listState
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${cardList.size} cards found",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sort button
                IconButton(onClick = { showSortDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort results",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(cardList) { card ->
            CardView(
                card,
                onCardClick,
                showIcon = false,
                onToggleEnable = { onToggleEnable(card) })
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
    listState: LazyListState = rememberLazyListState(),
    isDismissEnabled: Boolean,
    onCardDismissed: (Card) -> Unit,
    paddingValues: PaddingValues
) {
    Log.i(
        "KingdomList",
        "randomCards: ${kingdom.randomCards.size}, basicCards: ${kingdom.basicCards.size}, dependentCards: ${kingdom.dependentCards.size}, startingCards: ${kingdom.startingCards.size}, landscapeCards: ${kingdom.landscapeCards.size}"
    )

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL)
    ) {

        // RANDOM CARDS
        item {
            CardSpacer("Supply Cards")
        }
        items(
            items = kingdom.randomCards.keys.toList(),
            key = { card -> card.id }
        ) { card ->
            if (isDismissEnabled)
                DismissableCard(card, onCardDismissed, onCardClick, Modifier.animateItem())
            else {
                CardView(
                    card,
                    onCardClick,
                    enabled = true,
                    showIcon = true,
                    kingdom.randomCards[card]!!
                )
            }
        }

        // LANDSCAPE CARDS
        if (kingdom.hasLandscapeCards()) {
            item {
                CardSpacer("Landscape Cards")
            }
            items(
                items = kingdom.landscapeCards.keys.toList(),
                key = { card -> card.id }
            ) { card ->
                if (isDismissEnabled)
                    DismissableCard(card, onCardDismissed, onCardClick, Modifier.animateItem())
                else {
                    CardView(
                        card,
                        onCardClick,
                        enabled = true,
                        showIcon = true,
                        kingdom.landscapeCards[card]!!
                    )
                }
            }
        }

        // DEPENDENT CARDS
        if (kingdom.hasDependentCards()) {
            item {
                CardSpacer("Additional Cards")
            }
            items(kingdom.dependentCards.keys.toList()) { card ->
                CardView(
                    card,
                    onCardClick,
                    enabled = true,
                    showIcon = true,
                    kingdom.dependentCards[card]!!
                )
            }
        }

        // STARTING CARDS
        item {
            CardSpacer("Starting Cards")
        }
        items(kingdom.startingCards.keys.toList()) { card ->
            CardView(
                card,
                onCardClick,
                enabled = true,
                showIcon = true,
                kingdom.startingCards[card]!!
            )
        }

        // BASIC CARDS
        item {
            CardSpacer("Basic Cards")
        }
        item {
            PlayerSelectionButtons(
                selectedPlayers = selectedPlayers,
                onPlayerSelected = { onPlayerCountChange(it) }
            )
        }
        items(kingdom.basicCards.keys.toList()) { card ->
            CardView(card, onCardClick, enabled = true, showIcon = true, kingdom.basicCards[card]!!)
        }
    }
}

@Composable
fun PlayerSelectionButtons(selectedPlayers: Int, onPlayerSelected: (Int) -> Unit) {
    val playerCounts = listOf(2, 3, 4)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Constants.PADDING_SMALL),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        playerCounts.forEach { count ->
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
    }
}

@Composable
fun DismissableCard(
    card: Card,
    onCardDismissed: (Card) -> Unit,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            val dismissed = dismissValue != SwipeToDismissBoxValue.Settled
            if (dismissed) {
                onCardDismissed(card)
            }
            dismissed
        },
        positionalThreshold = { it * 0.25f } // Distance to be dismissed
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {

            // Change scale of icon depending on position
            val scale by animateDpAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75.dp else 1.dp,
                label = "icon scale"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                // Icon  behind the swipe
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Dismiss Icon",
                    modifier = Modifier.scale(scale.value),
                    tint = Color.White
                )
            }
        }
    ) {
        CardView(card, onCardClick)
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
    onCardClick: (Card) -> Unit,
    enabled: Boolean = true,
    showIcon: Boolean = true,
    amount: Int = 1,
    onToggleEnable: () -> Unit = {}
) {
    val focusManager: FocusManager = LocalFocusManager.current

    Card(
        onClick = {
            focusManager.clearFocus()
            onCardClick(card)
        },
        modifier = Modifier
            .height(Constants.CARD_HEIGHT)
            .alpha(if (enabled) 1f else 0.6f),
        enabled = enabled, // TODO: Card is not clickable when disabled
    ) {
        Row {
            // Vertical colored bar indicating card type
            ColoredBar(card.getColorByTypes())

            // Cropped card image
            CardImage(card)

            // Card name and price
            CardLabels(card, amount, modifier = Modifier.weight(1f))

            if (showIcon) {
                CardIcon(card.expansionImageId, card.sets[0].name)
            } else if (!card.basic && card.supply) {
                CardButton(card.isEnabled, onToggleEnable)
            }
        }
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
                .graphicsLayer {
                    if (card.sets.contains(Set.PLACEHOLDER)) {
                        if (card.name == "Trash Mat") {
                            scaleX = 1.75f
                            scaleY = 1.75f
                        } else {
                            scaleX = 1.25f
                            scaleY = 1.25f
                        }
                    } else {
                        scaleX = if (card.landscape) 2.1f else 2.5f
                        scaleY = if (card.landscape) 2.1f else 2.5f
                    }
                }
                .offset {
                    IntOffset(
                        x = 0,
                        y = when {
                            card.name == "Potion" || card.sets.contains(Set.PLACEHOLDER) -> 0
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
        Text(
            text = card.name + if (amount > 1) " ($amount)" else "",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = Constants.CARD_NAME_FONT_SIZE,
            color = MaterialTheme.colorScheme.onSurface
        )

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
            if (card.debt > 0) {
                ConditionalSpacer(Constants.PADDING_MINI)
                NumberHexagon(number = card.debt)
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
        AsyncImage(
            model = imageId,
            contentDescription = "$setName icon",
            modifier = Modifier
                .size(Constants.ICON_SIZE),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
    }
}

@Composable
fun CardButton(isEnabled: Boolean, onToggleEnable: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onToggleEnable() }
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.Filled.Remove else Icons.Default.Add,
            contentDescription = if (isEnabled) "Allowed" else "Banned",
            modifier = Modifier.size(Constants.ICON_SIZE),
            tint = if (isEnabled) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
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
private fun RadioButton(
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
            .clickable(onClick = onClick)
    )
}
