package dev.msuhr.dominionkingdoms.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.msuhr.dominionkingdoms.CurrentScreen
import dev.msuhr.dominionkingdoms.ui.DarkAgesMode
import dev.msuhr.dominionkingdoms.ui.PromoMode
import dev.msuhr.dominionkingdoms.ui.ProsperityMode
import dev.msuhr.dominionkingdoms.ui.RandomMode
import dev.msuhr.dominionkingdoms.ui.VetoMode
import dev.msuhr.dominionkingdoms.utils.Constants.PADDING_SMALL

object Constants {

    val CARD_DATA_VERSION = 1

    // UI TODO - SORT
    val PADDING_MINI = 4.dp
    val PADDING_SMALL = 8.dp
    val PADDING_MEDIUM = 16.dp
    val SETTING_ICON_SIZE = 30.dp
    val ICON_SIZE = 40.dp
    val CHECKMARK_SIZE = 30.dp
    // 72 or 88 would be preferable. 80 is used so that the dimensions of card artworks fit well
    val CARD_HEIGHT = 72.dp
    val CARD_HEIGHT_CARDS = 80.dp
    val COLOR_BAR_WIDTH = 8.dp
    val IMAGE_ROUNDED = 16.dp
    val CARD_IMAGE_WIDTH = 85.dp
    val CARD_NAME_FONT_SIZE = 20.sp
    val TEXT_SMALL = 16.sp
    val CARD_PRICE_SIZE = 24.dp
    val CARD_DEBT_SIZE = 26.dp

    // PREFERENCES
    const val USER_PREFERENCES_NAME = "settings_pref"

    // -> UserPrefsRepo?
    val DEFAULT_RANDOM_MODE = RandomMode.EVEN_AMOUNTS
    const val DEFAULT_RANDOM_EXPANSION_AMOUNT = 2
    val DEFAULT_VETO_MODE = VetoMode.REROLL_SAME
    const val DEFAULT_NUMBER_OF_CARDS_TO_GENERATE = 10
    const val MAX_CARDS_TO_GENERATE = 20 // TODO use
    const val DEFAULT_LANDSCAPE_COUNT = 2
    const val DEFAULT_LANDSCAPE_DIFFERENT_CATEGORIES = true
    val DEFAULT_DARK_AGES_STARTER_CARDS = DarkAgesMode.TEN_PERCENT_PER_CARD
    val DEFAULT_PROSPERITY_BASIC_CARDS = ProsperityMode.TEN_PERCENT_PER_CARD
    val DEFAULT_PROMO_MODE = PromoMode.POOL

    val START_DESTINATION = CurrentScreen.Kingdoms

    const val KINGDOM_NAME_MAX_LENGTH = 80

}

@Composable
fun calculatePadding(paddingValues: PaddingValues): PaddingValues {
    return PaddingValues(
        top = PADDING_SMALL + paddingValues.calculateTopPadding(),
        start = PADDING_SMALL,
        end = PADDING_SMALL,
        bottom = PADDING_SMALL + paddingValues.calculateBottomPadding()
    )
}