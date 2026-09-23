package dev.msuhr.dominionkingdoms.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.msuhr.dominionkingdoms.CardDependencyResolver
import dev.msuhr.dominionkingdoms.KingdomGenerator
import dev.msuhr.dominionkingdoms.model.Card
import dev.msuhr.dominionkingdoms.model.Kingdom
import kotlinx.coroutines.launch

/**
 * Minimal kingdom-generation demo UI. Lives in commonMain so it is
 * compile-checked by the Android target on every build; it is only wired up
 * in the iOS app (MainViewController). The Android app keeps its full UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DemoKingdomApp(cardsJson: String) {
    val cards = remember(cardsJson) { dev.msuhr.dominionkingdoms.model.CardJson.parseCards(cardsJson) }

    val cardDataSource = remember(cards) { InMemoryCardDataSource(cards) }
    val generator = remember(cards) {
        KingdomGenerator(
            cardDao = cardDataSource,
            expansionDao = InMemoryExpansionDataSource(cards),
            userPrefsRepository = InMemoryUserPrefs(),
            cardDependencyResolver = CardDependencyResolver(cardDataSource, InMemoryUserPrefs())
        )
    }

    var kingdom by remember { mutableStateOf<Kingdom?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Dominion Kingdoms - iOS Demo") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        try {
                            kingdom = generator.generateKingdom()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Generation failed"
                        }
                    }
                }
            ) {
                Text("Generate Kingdom")
            }

            val currentKingdom = kingdom
            if (currentKingdom == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Press the button to generate", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${cards.size} cards loaded from cards.json",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    currentKingdom.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (currentKingdom.landscapeCards.isNotEmpty()) {
                    Text(
                        "Landscapes: " + currentKingdom.landscapeCards.keys.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentKingdom.randomCards.keys.toList()) { card ->
                        DemoCardTile(card)
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoCardTile(card: Card) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color = card.getColorByTypes().first(), shape = RoundedCornerShape(4.dp))
        )
        Column {
            Text(card.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                buildString {
                    card.cost?.let { append("$it") }
                    if (card.potion) append(" P")
                    if (card.debt > 0) append(" ${card.debt}D")
                    append("  ")
                    append(card.types.firstOrNull()?.displayText ?: card.types.firstOrNull()?.name?.lowercase()?.replaceFirstChar { it.uppercaseChar() } ?: "")
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
