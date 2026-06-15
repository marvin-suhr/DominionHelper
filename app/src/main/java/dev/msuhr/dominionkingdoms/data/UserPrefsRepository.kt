package dev.msuhr.dominionkingdoms.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dev.msuhr.dominionkingdoms.model.*
import dev.msuhr.dominionkingdoms.ui.DarkAgesMode
import dev.msuhr.dominionkingdoms.ui.ProsperityMode
import dev.msuhr.dominionkingdoms.ui.RandomMode
import dev.msuhr.dominionkingdoms.ui.VetoMode
import dev.msuhr.dominionkingdoms.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.msuhr.dominionkingdoms.ui.PromoMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.USER_PREFERENCES_NAME)

object UserPreferencesKeys {
    val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode_preference")

    val USE_SYSTEM_THEME = booleanPreferencesKey("use_system_theme_preference")

    val RANDOM_MODE = stringPreferencesKey("random_mode_preference")
    val RANDOM_EXPANSION_AMOUNT = intPreferencesKey("random_expansion_amount_preference")

    val VETO_MODE = stringPreferencesKey("veto_mode_preference")
    val ALLOW_VETOING = booleanPreferencesKey("allow_vetoing_preference")
    val NUMBER_OF_CARDS_TO_GENERATE = intPreferencesKey("number_of_cards_to_generate_preference")

    val LANDSCAPE_COUNT = intPreferencesKey("landscape_categories_preference")
    val LANDSCAPE_DIFFERENT_CATEGORIES = booleanPreferencesKey("landscape_different_categories_preference")
    val PICK_LANDSCAPES_FROM_ANY_OWNED = booleanPreferencesKey("pick_landscapes_from_any_owned_preference")

    val DARK_AGES_STARTER_CARDS = stringPreferencesKey("dark_ages_starter_preference")
    val PROSPERITY_BASIC_CARDS = stringPreferencesKey("prosperity_basic_preference")
    val PROMO_MODE = stringPreferencesKey("promo_mode_preference")

    val KINGDOM_SORT_TYPE = stringPreferencesKey("kingdom_sort_type")

    val GENERATION_RULES = stringPreferencesKey("generation_rules")
    val LANDSCAPE_RULES = stringPreferencesKey("landscape_rules")

    val SHOW_DATABASE_RESET_DIALOG = booleanPreferencesKey("show_database_reset_dialog")

    val KINGDOM_GRID_VIEW = booleanPreferencesKey("kingdom_grid_view")
}

@Singleton
class UserPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = false // Fail on unknown keys to catch typos/structural issues
        coerceInputValues = true // Use default values for missing fields
        encodeDefaults = true
    }

    // Dark mode: null means use system default, true = dark, false = light
    val isDarkMode: Flow<Boolean?> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.IS_DARK_MODE]
        }

    suspend fun setDarkMode(isDarkMode: Boolean?) {
        context.dataStore.edit { settings ->
            if (isDarkMode == null) {
                settings.remove(UserPreferencesKeys.IS_DARK_MODE)
            } else {
                settings[UserPreferencesKeys.IS_DARK_MODE] = isDarkMode
            }
        }
    }

    // Use system theme: true = use system colors, false = use custom app colors
    val useSystemTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.USE_SYSTEM_THEME] ?: false // Default to false
        }

    suspend fun setUseSystemTheme(useSystem: Boolean) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.USE_SYSTEM_THEME] = useSystem
        }
    }

    val randomMode: Flow<RandomMode> = context.dataStore.data
        .map { preferences ->
            // Read the string value, defaulting to the name of your default enum constant
            val modeName = preferences[UserPreferencesKeys.RANDOM_MODE] ?: Constants.DEFAULT_RANDOM_MODE.name
            try {
                RandomMode.valueOf(modeName) // Convert string back to enum
            } catch (e: IllegalArgumentException) {
                // Handle cases where the stored string might be invalid (e.g., if you renamed enum constants)
                Constants.DEFAULT_RANDOM_MODE // Fallback to default
            }
        }

    suspend fun setRandomMode(newMode: RandomMode) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.RANDOM_MODE] = newMode.name
        }
    }

    val randomExpansionAmount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.RANDOM_EXPANSION_AMOUNT] ?: Constants.DEFAULT_RANDOM_EXPANSION_AMOUNT
        }

    suspend fun setRandomExpansionAmount(amount: Int) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.RANDOM_EXPANSION_AMOUNT] = amount
        }
    }

    // Veto mode
    val vetoMode: Flow<VetoMode> = context.dataStore.data
        .map { preferences ->
            val modeName = preferences[UserPreferencesKeys.VETO_MODE] ?: Constants.DEFAULT_VETO_MODE.name
            try {
                VetoMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                Constants.DEFAULT_VETO_MODE
            }
        }

    suspend fun setVetoMode(newMode: VetoMode) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.VETO_MODE] = newMode.name
        }
    }

    // Allow vetoing
    val allowVetoing: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.ALLOW_VETOING] ?: false // Default to false
        }

    suspend fun setAllowVetoing(allow: Boolean) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.ALLOW_VETOING] = allow
        }
    }

    // Number of cards to generate
    val numberOfCardsToGenerate: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.NUMBER_OF_CARDS_TO_GENERATE] ?: Constants.DEFAULT_NUMBER_OF_CARDS_TO_GENERATE
        }

    suspend fun setNumberOfCardsToGenerate(amount: Int) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.NUMBER_OF_CARDS_TO_GENERATE] = amount
        }
    }

    // Landscape categories
    val landscapeCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.LANDSCAPE_COUNT] ?: Constants.DEFAULT_LANDSCAPE_COUNT
        }

    suspend fun setLandscapeCount(amount: Int) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.LANDSCAPE_COUNT] = amount
        }
    }

    // Landscape different categories
    val landscapeDifferentCategories: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.LANDSCAPE_DIFFERENT_CATEGORIES] ?: Constants.DEFAULT_LANDSCAPE_DIFFERENT_CATEGORIES
        }

    suspend fun setLandscapeDifferentCategories(isDifferent: Boolean) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.LANDSCAPE_DIFFERENT_CATEGORIES] = isDifferent
        }
    }

    val pickLandscapesFromAnyOwned: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.PICK_LANDSCAPES_FROM_ANY_OWNED] ?: true
        }

    suspend fun setPickLandscapesFromAnyOwned(pickAny: Boolean) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.PICK_LANDSCAPES_FROM_ANY_OWNED] = pickAny
        }
    }

    // Dark Ages starter cards
    val darkAgesStarterCardsMode: Flow<DarkAgesMode> = context.dataStore.data
        .map { preferences ->
            val modeName = preferences[UserPreferencesKeys.DARK_AGES_STARTER_CARDS] ?: Constants.DEFAULT_DARK_AGES_STARTER_CARDS.name
            try {
                DarkAgesMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                Constants.DEFAULT_DARK_AGES_STARTER_CARDS
            }
        }

    suspend fun setDarkAgesStarterCardsMode(newMode: DarkAgesMode) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.DARK_AGES_STARTER_CARDS] = newMode.name
        }
    }

    // Prosperity starter cards
    val prosperityBasicCardsMode: Flow<ProsperityMode> = context.dataStore.data
        .map { preferences ->
            val modeName = preferences[UserPreferencesKeys.PROSPERITY_BASIC_CARDS] ?: Constants.DEFAULT_PROSPERITY_BASIC_CARDS.name
            try {
                ProsperityMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                Constants.DEFAULT_PROSPERITY_BASIC_CARDS
            }
        }

    suspend fun setProsperityBasicCardsMode(newMode: ProsperityMode) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.PROSPERITY_BASIC_CARDS] = newMode.name
        }
    }

    val promoMode: Flow<PromoMode> = context.dataStore.data
        .map { preferences ->
            val modeName = preferences[UserPreferencesKeys.PROMO_MODE] ?: Constants.DEFAULT_PROMO_MODE.name
            try {
                PromoMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                Constants.DEFAULT_PROMO_MODE
            }
        }

    suspend fun setPromoMode(newMode: PromoMode) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.PROMO_MODE] = newMode.name
        }
    }

    val kingdomSortType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.KINGDOM_SORT_TYPE] ?: "EXPANSION"
        }

    suspend fun setKingdomSortType(newSortType: String) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.KINGDOM_SORT_TYPE] = newSortType
        }
    }

    // Kingdom grid view preference
    val kingdomGridView: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.KINGDOM_GRID_VIEW] ?: true // Default to grid view
        }

    suspend fun setKingdomGridView(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.KINGDOM_GRID_VIEW] = enabled
        }
    }

    // Generation Rules (stored as Map<String, RuleOption> serialized to JSON via Kotlinx serialization)
    val activeRules: Flow<Map<String, RuleOption>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[UserPreferencesKeys.GENERATION_RULES] ?: "{}"
            try {
                json.decodeFromString<Map<String, RuleOption>>(jsonString)
            } catch (e: Exception) {
                emptyMap()
            }
        }

    suspend fun setRuleOption(ruleId: String, option: RuleOption) {
        context.dataStore.edit { settings ->
            val currentJson = settings[UserPreferencesKeys.GENERATION_RULES] ?: "{}"
            val currentMap: MutableMap<String, RuleOption> = try {
                json.decodeFromString<Map<String, RuleOption>>(currentJson).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
            currentMap[ruleId] = option
            settings[UserPreferencesKeys.GENERATION_RULES] = json.encodeToString(currentMap)
        }
    }

    // Landscape Rules (stored as Map<String, Boolean> - whether each landscape type is enabled)
    // Default: all landscape types enabled
    val landscapeRules: Flow<Map<String, Boolean>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[UserPreferencesKeys.LANDSCAPE_RULES] ?: null
            if (jsonString == null) {
                // Default: all landscape types enabled
                mapOf(
                    "landscape_event" to true,
                    "landscape_landmark" to true,
                    "landscape_project" to true,
                    "landscape_trait" to true,
                    "landscape_way" to true
                )
            } else {
                try {
                    json.decodeFromString<Map<String, Boolean>>(jsonString)
                } catch (e: Exception) {
                    emptyMap()
                }
            }
        }

    suspend fun setLandscapeRule(ruleId: String, enabled: Boolean) {
        context.dataStore.edit { settings ->
            val currentJson = settings[UserPreferencesKeys.LANDSCAPE_RULES]
            val currentMap: MutableMap<String, Boolean> = if (currentJson != null) {
                try {
                    json.decodeFromString<Map<String, Boolean>>(currentJson).toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            } else {
                // Default: all landscape types enabled
                mutableMapOf(
                    "landscape_event" to true,
                    "landscape_landmark" to true,
                    "landscape_project" to true,
                    "landscape_trait" to true,
                    "landscape_way" to true
                )
            }
            currentMap[ruleId] = enabled
            settings[UserPreferencesKeys.LANDSCAPE_RULES] = json.encodeToString(currentMap)
        }
    }

    suspend fun resetGenerationRules() {
        context.dataStore.edit { settings ->
            settings.remove(UserPreferencesKeys.GENERATION_RULES)
            settings.remove(UserPreferencesKeys.LANDSCAPE_RULES)
        }
    }

    val showDatabaseResetDialog: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[UserPreferencesKeys.SHOW_DATABASE_RESET_DIALOG] ?: false
        }

    suspend fun setShowDatabaseResetDialog(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[UserPreferencesKeys.SHOW_DATABASE_RESET_DIALOG] = show
        }
    }
}
