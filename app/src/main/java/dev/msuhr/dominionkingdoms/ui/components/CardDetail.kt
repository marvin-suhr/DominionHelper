package dev.msuhr.dominionkingdoms.ui.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.utils.findIndexOfReference
import dev.msuhr.dominionkingdoms.utils.getDrawableId
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

    val initialIndex = findIndexOfReference(cardList, initialCard)

    Log.i("CardDetailPager", "Initialized Pager with ${cardList.joinToString (", ") { if (it.name == "Village")  it.toString() else "" }}")
    Log.i("CardDetailPager", "Initial card: ${initialCard}")
    Log.i("CardDetailPager", "Initial index: $initialIndex")

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
                CardDetail(card = cardForPage, onClick = onClick, onFavorite = onFavorite, onBan = onBan)
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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AsyncImage(
                model = drawableId,
                contentDescription = "Card Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        Log.d("CardDetail", "CardDetail Column clicked!") // For debugging
                        onClick()
                    },
                contentScale = ContentScale.FillWidth
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onFavorite(card) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (card.isFavorite) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    imageVector = if (card.isFavorite) {
                        Icons.Outlined.Star
                    } else {
                        Icons.Filled.Star
                    },
                    contentDescription = if (card.isFavorite) "Unfavorite" else "Favorite" + " this card",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.padding(start = 4.dp))
                Text(if (card.isFavorite) "Unfavorite" else "Favorite")
            }

            Button(
                onClick = { onBan(card) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (card.isEnabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                ),
                enabled = card.supply
            ) {
                Icon(
                    imageVector = if (card.isEnabled) {
                        Icons.Filled.Block
                    } else {
                        Icons.Filled.Check
                    },
                    contentDescription = if (card.isEnabled) "Ban" else "Unban" + " this card",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.padding(start = 4.dp))
                Text(if (card.isEnabled) "Ban" else "Unban")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            card.categories.forEach { category ->
                CategoryBubble(category.displayName)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CategoryBubble(categoryName: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        Text(
            text = categoryName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
