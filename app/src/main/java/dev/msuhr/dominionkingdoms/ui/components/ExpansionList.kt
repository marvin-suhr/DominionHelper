package dev.msuhr.dominionkingdoms.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import dev.msuhr.dominionkingdoms.model.ExpansionWithEditions
import dev.msuhr.dominionkingdoms.utils.Constants
import dev.msuhr.dominionkingdoms.utils.getDrawableId
import dev.msuhr.dominionkingdoms.R

// Display an expansion item
@Composable
fun ExpansionListItem(
    expansion: ExpansionWithEditions,
    portraitCount: Int,
    landscapeCount: Int,
    onClick: () -> Unit,
    onOwnershipToggle: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
            ),
        shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Constants.CARD_HEIGHT)
                .padding(horizontal = 16.dp, vertical = 12.dp), // Inner padding
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpansionImage(expansion)
            ExpansionLabels(expansion, portraitCount, landscapeCount, Modifier.weight(1f))
            ExpansionOwnershipIcon(expansion, onOwnershipToggle)
        }
    }
}

// Display expansion image (1st or 2nd edition depending on ownership)
@Composable
fun ExpansionImage(expansion: ExpansionWithEditions) {
    val context = LocalContext.current
    val drawableId = getDrawableId(context, expansion.displayImageName)

    Image(
        painter = painterResource(id = drawableId),
        contentDescription = "${expansion.name} Expansion Image",
        modifier = Modifier.size(Constants.ICON_SIZE),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
    )
}

@Composable
fun ExpansionLabels(
    expansion: ExpansionWithEditions,
    portraitCount: Int,
    landscapeCount: Int,
    modifier: Modifier = Modifier
) {
    val inlineContent = remember {
        val placeholder = Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextBottom)
        mapOf(
            "p" to InlineTextContent(placeholder) { Image(painterResource(R.drawable.portrait2), null, Modifier.size(14.dp), alpha = 0.6f, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)) },
            "l" to InlineTextContent(placeholder) { Image(painterResource(R.drawable.landscape2), null, Modifier.size(15.dp), alpha = 0.6f, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)) }
        )
    }

    Column(modifier = modifier) {
        Text(
            // Constructs card amount text with icons
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = Constants.CARD_NAME_FONT_SIZE, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(expansion.displayName)
                }
                if (expansion.isAnyOwned()) {
                    withStyle(SpanStyle(fontSize = 14.sp/*Constants.TEXT_SMALL*/, fontStyle = FontStyle.Italic, color = LocalContentColor.current.copy(alpha = 0.6f))) {
                        if (portraitCount > 0) { append(" $portraitCount "); appendInlineContent("p", "[p]") }
                        if (landscapeCount > 0) { append(" $landscapeCount "); appendInlineContent("l", "[l]") }
                    }
                }
            },
            inlineContent = inlineContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        val yearText = if (expansion.isBothOwned()) "" else expansion.activeEdition?.let { "${it.year} - " } ?: ""
        Text(
            //text = expansion.ownershipText + yearText,
            text = yearText + expansion.ownershipText,
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
    onOwnershipToggle: () -> Unit
) {
    // TODO make a BothOwnedIcon, FirstEditionIcon, SecondEditionIcon, UnownedIcon to reduce nesting and repetition
    Box(
        modifier = Modifier
            .size(35.dp), // TODO random
        contentAlignment = Alignment.Center
    ) {
        // Multi-edition expansion: show ownership state icon
        if (expansion.hasMultipleEditions) {
            when {
                expansion.isBothOwned() -> {
                    Box {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.fillMaxSize().clip(CircleShape).clickable { onOwnershipToggle() }, MaterialTheme.colorScheme.primary)
                        if (expansion.isSharedSecondEdition) SharedEditionIndicator()
                    }
                }
                expansion.isFirstEditionOwned -> CircleWithNumber(1, onOwnershipToggle)
                expansion.isSecondEditionOwned -> {
                    Box {
                        CircleWithNumber(2, onOwnershipToggle)
                        if (expansion.isSharedSecondEdition) SharedEditionIndicator()
                    }
                }
                else -> Icon(Icons.Outlined.Circle, null, Modifier.fillMaxSize().clip(CircleShape).clickable { onOwnershipToggle() }, MaterialTheme.colorScheme.outline)
            }
        } else {
            val isOwned = expansion.isAnyOwned()
            Icon(
                imageVector = if (isOwned) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { onOwnershipToggle() },
                tint = if (isOwned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun CircleWithNumber(number: Int, onOwnershipToggle: () -> Unit) {
    Box(modifier = Modifier.size(Constants.ICON_SIZE), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Circle, null, Modifier.fillMaxSize().clip(CircleShape).clickable { onOwnershipToggle() }, MaterialTheme.colorScheme.primary)
        Text(number.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun SharedEditionIndicator() {
    Icon(Icons.Filled.Link, null, Modifier.size(12.dp).offset { IntOffset(16, -16) }, MaterialTheme.colorScheme.primary)
}

@Composable
private fun ManagementCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
            .shadow(
              elevation = 4.dp,
                shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
            ),
        shape = RoundedCornerShape(Constants.IMAGE_ROUNDED)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Constants.CARD_HEIGHT)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Icon area
            Box(
                modifier = Modifier.size(Constants.ICON_SIZE),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = Constants.CARD_NAME_FONT_SIZE,
                    //fontWeight = FontWeight.Bold,
                    color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = Constants.TEXT_SMALL,
                    color = LocalContentColor.current.copy(alpha = if (enabled) 0.6f else 0.38f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing Chevron area
            Box(
                modifier = Modifier.size(Constants.CHECKMARK_SIZE),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (enabled) LocalContentColor.current.copy(alpha = 0.6f) else LocalContentColor.current.copy(alpha = 0.38f),
                    modifier = Modifier.size(Constants.ICON_SIZE)
                )
            }
        }
    }
}

@Composable
fun PromoCardsListItem(promoCardCountText: String, onClick: () -> Unit) {
    ManagementCard(
        title = "Promo Cards",
        subtitle = promoCardCountText,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.set_promo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Constants.ICON_SIZE)
            )
        },
        onClick = onClick
    )
}

@Composable
fun FavoriteCardsListItem(favoriteCardCount: Int, onClick: () -> Unit) {
    ManagementCard(
        title = "Favorite Cards",
        subtitle = "$favoriteCardCount favorite cards",
        icon = {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (favoriteCardCount > 0) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = 0.38f),
                modifier = Modifier.size(Constants.ICON_SIZE)
            )
        },
        onClick = onClick,
        enabled = favoriteCardCount > 0
    )
}

@Composable
fun BlacklistedCardsListItem(disabledCardCount: Int, onClick: () -> Unit) {
    ManagementCard(
        title = "Blacklisted Cards",
        subtitle = "$disabledCardCount blacklisted cards",
        icon = {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = null,
                tint = if (disabledCardCount > 0) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f),
                modifier = Modifier.size(Constants.ICON_SIZE)
            )
        },
        onClick = onClick,
        enabled = disabledCardCount > 0
    )
}
