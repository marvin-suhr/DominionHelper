package com.marvinsuhr.dominionhelper.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.marvinsuhr.dominionhelper.model.ExpansionWithEditions
import com.marvinsuhr.dominionhelper.utils.Constants
import com.marvinsuhr.dominionhelper.utils.getDrawableId

// TODO: Check Box contentAlignment vs contents Modifier.align (first is better)

// Display an expansion item
@Composable
fun ExpansionListItem(
    expansion: ExpansionWithEditions,
    onClick: () -> Unit, // Click on the whole item goes to detail
    onOwnershipToggle: () -> Unit, // Callback for single edition toggle click
    hasMultipleEditions: Boolean,
    onToggleExpansion: () -> Unit // Callback for clicking the arrow
) {
    Card(
        modifier = Modifier
            .height(Constants.CARD_HEIGHT)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Expansion image
            ExpansionImage(expansion)

            // Expansion name and additional text
            ExpansionLabels(
                expansion, Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )

            // Ownership toggle
            ExpansionOwnershipIcon(
                expansion,
                hasMultipleEditions,
                onToggleExpansion,
                onOwnershipToggle
            )
        }
    }
}

// Display expansion image (1st or 2nd edition depending on ownership)
@Composable
fun ExpansionImage(expansion: ExpansionWithEditions) {
    val context = LocalContext.current

    // Determine which edition image to show based on ownership
    val isFirstOwned = expansion.firstEdition?.isOwned == true
    val isSecondOwned = expansion.secondEdition?.isOwned == true

    val imageName = when {
        // If only first edition is owned, show first edition image
        isFirstOwned && !isSecondOwned -> expansion.firstEdition.imageName
        // Otherwise show second edition image (NONE, SECOND, or BOTH owned)
        else -> expansion.secondEdition?.imageName ?: expansion.firstEdition?.imageName // Error if 2nd not found?
    }

    val drawableId = getDrawableId(context, imageName ?: "")

    AsyncImage(
        model = drawableId,
        contentDescription = "${expansion.name} Expansion Image",
        modifier = Modifier
            .aspectRatio(1f)
            .padding(Constants.PADDING_MEDIUM),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
    )
}

@Composable
fun ExpansionLabels(
    expansion: ExpansionWithEditions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(Constants.PADDING_SMALL)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = Constants.CARD_NAME_FONT_SIZE, fontWeight = FontWeight.Bold)) {
                    append(expansion.name)
                }
                withStyle(SpanStyle(fontSize = Constants.TEXT_SMALL, fontStyle = FontStyle.Italic)) {
                    append(" (${expansion.firstEdition?.year})")
                }
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = Constants.CARD_NAME_FONT_SIZE,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = expansion.firstEdition?.size?.text + " expansion",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = Constants.TEXT_SMALL,
            color = LocalContentColor.current.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ExpansionOwnershipIcon(
    expansion: ExpansionWithEditions,
    hasMultipleEditions: Boolean,
    onToggleExpansion: () -> Unit, // TODO Deleting this breaks toggling??
    onOwnershipToggle: () -> Unit // Check both of these
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f),
        onClick = {
            if (hasMultipleEditions) {
                Log.i("ExpansionListItem", "Clicking multi-edition ownership toggle")
                onOwnershipToggle()
            } else {
                Log.i("ExpansionListItem", "Clicking ownership icon")
                onOwnershipToggle()
            }
        },
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (hasMultipleEditions) {
                // Multi-edition expansion: show ownership state icon
                val isFirstOwned = expansion.firstEdition?.isOwned == true
                val isSecondOwned = expansion.secondEdition?.isOwned == true

                when {
                    isFirstOwned && isSecondOwned -> {
                        // BOTH editions owned - show checkmark
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Both Editions Owned",
                            modifier = Modifier.size(Constants.ICON_SIZE),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    isFirstOwned -> {
                        CircleWithNumber(1)
                    }
                    isSecondOwned -> {
                        CircleWithNumber(2)
                    }
                    else -> {
                        // NONE owned - show empty circle
                        Icon(
                            imageVector = Icons.Outlined.Circle,
                            contentDescription = "Unowned",
                            modifier = Modifier.size(Constants.ICON_SIZE),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Single edition expansion
                val isOwned = expansion.firstEdition?.isOwned == true || expansion.secondEdition?.isOwned == true
                Icon(
                    imageVector = if (isOwned) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (isOwned) "Owned" else "Unowned",
                    modifier = Modifier.size(Constants.ICON_SIZE),
                    tint = if (isOwned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CircleWithNumber(number: Int) {
    Box(
        modifier = Modifier.size(Constants.ICON_SIZE),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Circle,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = number.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}

// TODO: Abstraction over favorite and blacklisted cards list
@Composable
fun FavoriteCardsListItem(
    favoriteCardCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(Constants.CARD_HEIGHT)
            .then(
                if (favoriteCardCount > 0) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Favorite Cards",
                tint = if (favoriteCardCount > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current.copy(alpha = 0.38f)
                },
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(Constants.PADDING_MEDIUM)
            )

            // Text
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Constants.PADDING_SMALL)
            ) {
                Text(
                    text = "Favorite Cards",
                    fontSize = Constants.CARD_NAME_FONT_SIZE,
                    color = if (favoriteCardCount > 0) {
                        LocalContentColor.current
                    } else {
                        LocalContentColor.current.copy(alpha = 0.38f)
                    }
                )
                Text(
                    text = "$favoriteCardCount favorite cards",
                    fontSize = Constants.TEXT_SMALL,
                    color = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }

            // Trailing chevron icon
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View favorite cards",
                    tint = if (favoriteCardCount > 0) {
                        LocalContentColor.current
                    } else {
                        LocalContentColor.current.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(Constants.ICON_SIZE)
                )
            }
        }
    }
}


@Composable
fun BlacklistedCardsListItem(
    disabledCardCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(Constants.CARD_HEIGHT)
            .then(
                if (disabledCardCount > 0) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = "Blacklisted Cards",
                tint = if (disabledCardCount > 0) {
                    LocalContentColor.current
                } else {
                    LocalContentColor.current.copy(alpha = 0.38f)
                },
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(Constants.PADDING_MEDIUM)
            )

            // Text
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Constants.PADDING_SMALL)
            ) {
                Text(
                    text = "Blacklisted Cards",
                    fontSize = Constants.CARD_NAME_FONT_SIZE,
                    color = if (disabledCardCount > 0) {
                        LocalContentColor.current
                    } else {
                        LocalContentColor.current.copy(alpha = 0.38f)
                    }
                )
                Text(
                    text = "$disabledCardCount blacklisted cards",
                    fontSize = Constants.TEXT_SMALL,
                    color = LocalContentColor.current.copy(alpha = 0.6f)
                )
            }

            // Trailing chevron icon
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View blacklisted cards",
                    tint = if (disabledCardCount > 0) {
                        LocalContentColor.current
                    } else {
                        LocalContentColor.current.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(Constants.ICON_SIZE)
                )
            }
        }
    }
}
