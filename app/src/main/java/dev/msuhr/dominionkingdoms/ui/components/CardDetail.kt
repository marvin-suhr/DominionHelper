package dev.msuhr.dominionkingdoms.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.Type
import dev.msuhr.dominionkingdoms.utils.Constants
import dev.msuhr.dominionkingdoms.utils.getDrawableId
import dev.msuhr.dominionkingdoms.utils.ui.horizontalFadingEdges
import kotlinx.coroutines.flow.distinctUntilChanged

// Show a pager scrolling through a list of cards
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardDetailPager(
    modifier: Modifier = Modifier,
    cardList: List<Card>,
    initialCard: Card,
    onClick: () -> Unit,
    onPageChanged: (Card) -> Unit,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onFavorite: (Card) -> Unit = {},
    onBan: (Card) -> Unit = {}
) {

    if (cardList.isEmpty()) {
        Log.w("CardDetailPager", "Card list is empty, cannot initialize Pager.")
        return
    }

    val initialIndex = cardList.indexOf(initialCard)
    // I think there was a point to using reference equality here
    //val initialIndex = findIndexOfReference(cardList, initialCard)

    val pagerState =
        rememberPagerState(initialPage = initialIndex, pageCount = { cardList.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page >= 0 && page < cardList.size) {
                    val currentCard = cardList[page]
                    Log.i("CardDetailPager", "Page changed to: ${currentCard.name}")
                    onPageChanged(currentCard)
                }
            }
    }

    Column(
        modifier = modifier.padding(paddingValues)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->

            // It's possible for 'page' to be temporarily out of bounds during fast scrolls
            // or state restoration. Ensure it's valid.
            if (page >= 0 && page < cardList.size) {
                val cardForPage = cardList[page]
                Log.i("CardDetailPager", "Displaying ${cardForPage.name}, Index $page")
                CardDetail(
                    card = cardForPage,
                    onClick = onClick,
                    onFavorite = onFavorite,
                    onBan = onBan
                )
            } else {
                Log.w("CardDetailPager", "Page index $page is out of bounds for cardList size ${cardList.size}")
                // Optionally, display a placeholder or empty content
            }
        }
    }
}

// Show a detail view of a single card
@Composable
fun CardDetail(
    card: Card,
    onClick: () -> Unit,
    onFavorite: (Card) -> Unit,
    onBan: (Card) -> Unit
) {
    val drawableId = getDrawableId(LocalContext.current, card.imageName)

    // Dynamically track the loaded asset's aspect ratio
    var imageAspectRatio by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Keeps the layout clean and adaptable
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = drawableId,
                contentDescription = "Card Image",
                contentScale = ContentScale.Fit,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        val size = state.painter.intrinsicSize
                        if (size.width > 0 && size.height > 0) {
                            imageAspectRatio = size.width / size.height
                        }
                    }
                },
                modifier = Modifier
                    .padding(Constants.PADDING_SMALL)
                    .aspectRatio(
                        ratio = imageAspectRatio,
                        matchHeightConstraintsFirst = imageAspectRatio < 1f
                    )
                    .clip(RoundedCornerShape(Constants.IMAGE_ROUNDED))
                    .clickable { onClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val interceptor = rememberHorizontalScrollInterception()

        // Row of scrollable card categories
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp) // Chip height
                .horizontalFadingEdges(fadeWidth = 16.dp)
                .nestedScroll(interceptor),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // This the only way I can get it to look the way I want
            item { Spacer(modifier = Modifier.width(0.dp)) }
            items(card.categories) { category ->
                CategoryChip(category.displayName)
            }
            item { Spacer(modifier = Modifier.width(0.dp)) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Second Row: (Un)favorite and (Un)ban buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // No favorite / ban buttons for tokens and mats
            if (!card.types.contains(Type.TOKEN) && !card.types.contains(Type.MAT) && !card.types.contains(Type.PILE)) {

                // Favorite toggle
                if (card.isFavorite) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onFavorite(card) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,//Color(0xFFFFD54F), // TODO: Light mode colors
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer//Color(0xFF121212)
                        )
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unfavorite", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onFavorite(card) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.outline//Color(0xB3FFFFFF)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF424242))
                    ) {
                        Icon(Icons.Outlined.StarBorder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Favorite", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Can't ban basic cards
                if (!card.basic && card.supply) {

                    // Ban toggle
                    if (!card.isEnabled) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { onBan(card) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,//Color(0xFFE57373), // TODO: Light mode colors
                                contentColor = MaterialTheme.colorScheme.onErrorContainer//Color(0xFF121212)
                            )
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unban", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onBan(card) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.outline//Color(0xB3FFFFFF)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF424242))
                        ) {
                            Icon(Icons.Outlined.Block, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ban", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// So the card category list scroll doesn't carry over to the pager
@Composable
fun rememberHorizontalScrollInterception(): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            // Intercept dragging/scrolling when boundaries are hit
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Return 'available' to pretend we consumed all leftover horizontal scroll
                return Offset(x = available.x, y = 0f)
            }

            // Intercept rapid flicking/flinging when boundaries are hit
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                // Return 'available' to pretend we consumed all leftover horizontal kinetic energy
                return Velocity(x = available.x, y = 0f)
            }
        }
    }
}

@Composable
fun CategoryChip(categoryName: String, modifier: Modifier = Modifier) {
    AssistChip(
        modifier = modifier,
        onClick = { /* Optional: Handle click if categories filter the library */ },
        label = {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = null // Removes the default outline border so it looks like a clean solid badge
    )
}
