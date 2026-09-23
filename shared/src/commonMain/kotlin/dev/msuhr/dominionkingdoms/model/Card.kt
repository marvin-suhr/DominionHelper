package dev.msuhr.dominionkingdoms.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "cards")
@Serializable
data class Card(
    @SerialName("id") @PrimaryKey val id: Int,
    @SerialName("name") val name: String,
    @SerialName("image_name") val imageName: String,

    @SerialName("sets") val sets: List<Set>,
    @SerialName("types") val types: List<Type>,
    @SerialName("categories") val categories: List<Category>,

    @SerialName("cost") val cost: Int?,
    @SerialName("overpay") val overpay: Boolean = false,
    @SerialName("special_cost") val specialCost: Boolean = false,
    @SerialName("potion") val potion: Boolean = false,
    @SerialName("debt") val debt: Int? = null,

    @SerialName("supply") val supply: Boolean = true,
    @SerialName("landscape") val landscape: Boolean = false,
    @SerialName("basic") val basic: Boolean = false,

    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("is_favorite") val isFavorite: Boolean = false
) {

    override fun toString(): String {
        return id.toString()
    }

    @Ignore
    var expansionImageName: String = if (sets.size >= 2) sets[1].imageName else sets[0].imageName

    fun getDisplayCategory(): CardDisplayCategory {
        return when {
            this.types.contains(Type.PILE) -> CardDisplayCategory.NONE
            this.landscape -> CardDisplayCategory.LANDSCAPE
            this.types.contains(Type.MAT) || this.types.contains(Type.TOKEN) -> CardDisplayCategory.MATERIAL
            !this.supply || this.basic -> CardDisplayCategory.SPECIAL
            else -> CardDisplayCategory.SUPPLY
        }
    }

    class CardTypeComparator(
        private val sortByCostAsTieBreaker: Boolean = false
    ) : Comparator<Card> {

        override fun compare(card1: Card, card2: Card): Int {
            val displayCategory1 = card1.getDisplayCategory()
            val displayCategory2 = card2.getDisplayCategory()

            // 1. Compare by main display category's ordinal (ensures SUPPLY < SPECIAL < LANDSCAPE < MATERIAL)
            val categoryComparison = displayCategory1.ordinal.compareTo(displayCategory2.ordinal)
            if (categoryComparison != 0) {
                return categoryComparison
            }

            // 2. Both cards are in the same DisplayCategory -> sort by sortPriority
            // This is only for non-supply cards. Other categories are sorted by name
            if (displayCategory1 != CardDisplayCategory.SUPPLY) {

                    // Find the minimum sortPriority for each card's types
                    val priority1 = card1.types.minOf { it.sortPriority }
                    val priority2 = card2.types.minOf { it.sortPriority }

                    val typePriorityComparison = priority1.compareTo(priority2)
                    if (typePriorityComparison != 0) {
                        return typePriorityComparison
                    }

                // 3. If this flag is set to true, sort by cost as a tie breaker
                // This is so that special cards in Base and Empires have a neat order
                if (sortByCostAsTieBreaker) {
                    val cost1 = card1.cost ?: Int.MAX_VALUE
                    val cost2 = card2.cost ?: Int.MAX_VALUE

                    val costComparison = cost1.compareTo(cost2)
                    if (costComparison != 0) {
                        return costComparison
                    }
                }
            }

            // 4. Sort by name as the final tie breaker
            return card1.name.compareTo(card2.name, ignoreCase = true)
        }
    }

    fun getColorByTypes(): List<Color> {
        val colors = mutableListOf<Color>()
        if (types.contains(Type.TREASURE)) {
            colors.add(Color(0xFFF7DC7E))
        }
        if (types.contains(Type.DURATION)) {
            colors.add(Color(0xFFE78845))
        }
        if (types.contains(Type.REACTION)) {
            colors.add(Color(0xFF67AAD9))
        }
        if (types.contains(Type.RESERVE)) {
            colors.add(Color(0xFFD7BC86))
        }
        if (types.contains(Type.VICTORY)) {
            if (types.contains(Type.ACTION)) {
                colors.add(Color(0xFFF3EEE2))
            }
            colors.add(Color(0xFFA2CB85))
        }
        if (types.contains(Type.CURSE)) {
            colors.add(Color(0xFFB18EBC))
        }
        if (types.contains(Type.RUINS)) {
            colors.add(Color(0xFF875F3C))
        }
        if (types.contains(Type.NIGHT)) {
            colors.add(Color(0xFF535353))
        }
        if (types.contains(Type.SHELTER)) {
            if (types.contains(Type.ACTION)) {
                colors.add(Color(0xFFF3EEE2))
            }
            colors.add(Color(0xFFEF876F))
        }

        // Default color
        if (colors.isEmpty()) {
            colors.add(Color(0xFFF3EEE2))
        }
        return colors
    }
}
