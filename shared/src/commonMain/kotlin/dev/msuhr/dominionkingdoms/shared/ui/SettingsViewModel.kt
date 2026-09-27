package dev.msuhr.dominionkingdoms.shared.ui

import dev.msuhr.dominionkingdoms.model.CardRules
import dev.msuhr.dominionkingdoms.model.DarkAgesMode
import dev.msuhr.dominionkingdoms.model.GenerationRule
import dev.msuhr.dominionkingdoms.model.PromoMode
import dev.msuhr.dominionkingdoms.model.ProsperityMode
import dev.msuhr.dominionkingdoms.model.RandomMode
import dev.msuhr.dominionkingdoms.model.RuleOption
import dev.msuhr.dominionkingdoms.model.VetoMode
import dev.msuhr.dominionkingdoms.shared.data.AppPrefsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed class SettingItem {
    data class SectionHeader(val title: String) : SettingItem()

    data class SwitchSetting(
        val title: String,
        val description: String? = null,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val imageName: String = ""
    ) : SettingItem()

    data class TextSetting(
        val title: String,
        val text: String,
        val onTextChange: (String) -> Unit
    ) : SettingItem()

    data class NumberSetting(
        val title: String,
        val number: Int,
        val min: Int,
        val max: Int,
        val onNumberChange: (Int) -> Unit
    ) : SettingItem()

    data class ChoiceSetting<E : Enum<E>>(
        val title: String,
        val selectedOption: E,
        val allOptions: List<E>,
        val optionDisplayFormatter: (E) -> String,
        val onOptionSelected: (E) -> Unit,
        val description: String? = null,
        val imageName: String = ""
    ) : SettingItem()

    data class FeedbackSetting(
        val title: String,
        val subtitle: String,
        val onClick: () -> Unit
    ) : SettingItem()

    data class NavigationSetting(
        val title: String,
        val description: String? = null,
        val onClick: () -> Unit
    ) : SettingItem()

    data class ActionSetting(
        val title: String,
        val description: String? = null,
        val onClick: () -> Unit,
        val isDangerous: Boolean = false
    ) : SettingItem()

    data class RangeRuleSetting(
        val title: String,
        val min: Int,
        val max: Int,
        val onRangeChange: (Int, Int) -> Unit,
        val imageName: String = ""
    ) : SettingItem()
}

enum class DarkModeSetting(val displayName: String) {
    SYSTEM("System default"),
    DARK("Dark"),
    LIGHT("Light")
}

enum class SettingsSubScreen {
    MAIN,
    CARD_TYPES,
    CARD_CATEGORIES,
    CARD_COSTS,
    LANDSCAPES
}

class SettingsViewModel(
    private val userPrefsRepository: AppPrefsSource
) : SharedViewModel(), ScreenViewModel {

    data class SettingsUiState(
        val settings: List<SettingItem> = emptyList(),
        val currentSubScreen: SettingsSubScreen = SettingsSubScreen.MAIN,
        val showResetRulesDialog: Boolean = false
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        getSettings()
            .onEach { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
            .launchIn(scope)
    }

    private fun getSettings(): Flow<List<SettingItem>> {
        return combine(
            userPrefsRepository.isDarkMode,
            userPrefsRepository.useSystemTheme,
            userPrefsRepository.randomMode,
            userPrefsRepository.randomExpansionAmount,
            userPrefsRepository.vetoMode,
            userPrefsRepository.allowVetoing,
            userPrefsRepository.numberOfCardsToGenerate,
            userPrefsRepository.landscapeCount,
            userPrefsRepository.landscapeDifferentCategories,
            userPrefsRepository.pickLandscapesFromAnyOwned,
            userPrefsRepository.darkAgesStarterCardsMode,
            userPrefsRepository.prosperityBasicCardsMode,
            userPrefsRepository.promoMode,
            userPrefsRepository.activeRules,
            userPrefsRepository.landscapeRules,
            _uiState.map { it.currentSubScreen }.distinctUntilChanged()
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val darkModePreference = values[0] as Boolean?
            val useSystemTheme = values[1] as Boolean
            val currentRandomMode = values[2] as RandomMode
            val currentRandomExpAmount = values[3] as Int
            val currentVetoMode = values[4] as VetoMode
            val currentAllowVetoing = values[5] as Boolean
            val currentNumCardsToGen = values[6] as Int
            val currentLandscapeCategories = values[7] as Int
            val currentLandscapeDiffCat = values[8] as Boolean
            val currentPickLandscapesAny = values[9] as Boolean
            val currentDarkAgesMode = values[10] as DarkAgesMode
            val currentProsperityMode = values[11] as ProsperityMode
            val currentPromoMode = values[12] as PromoMode
            @Suppress("UNCHECKED_CAST")
            val currentActiveRules = values[13] as Map<String, RuleOption>
            @Suppress("UNCHECKED_CAST")
            val currentLandscapeRules = values[14] as Map<String, Boolean>
            val currentSubScreen = values[15] as SettingsSubScreen

            val settings = mutableListOf<SettingItem>()

            when (currentSubScreen) {
                SettingsSubScreen.MAIN -> {
                    // Interface Section
                    settings.add(SettingItem.SectionHeader("Interface"))
                    settings.add(
                        SettingItem.ChoiceSetting(
                            title = "App theme",
                            selectedOption = if (darkModePreference == null) DarkModeSetting.SYSTEM
                            else if (darkModePreference) DarkModeSetting.DARK
                            else DarkModeSetting.LIGHT,
                            allOptions = DarkModeSetting.entries.toList(),
                            optionDisplayFormatter = { it.displayName },
                            onOptionSelected = { newMode ->
                                when (newMode) {
                                    DarkModeSetting.SYSTEM -> setDarkMode(null)
                                    DarkModeSetting.DARK -> setDarkMode(true)
                                    DarkModeSetting.LIGHT -> setDarkMode(false)
                                }
                            }
                        )
                    )
                    // Note: the Android app also offers "Dynamic color" on
                    // Android 12+. Material You does not exist on iOS, so the
                    // switch is omitted here.

                    // Generation Section
                    settings.add(SettingItem.SectionHeader("Kingdom generation"))

                    settings.add(
                        SettingItem.ChoiceSetting(
                            title = "Random mode",
                            selectedOption = currentRandomMode,
                            allOptions = RandomMode.entries.toList(),
                            optionDisplayFormatter = { it.displayName },
                            onOptionSelected = { setRandomMode(it) },
                            description =
                            """Choose how cards are selected.

Full Random: select cards completely randomly from selected expansions.

Limited Random: select a fixed number of expansions and randomly draw cards from them.

Even Amounts: select equal card amounts from each selected expansion."""
                        )
                    )
                    if (currentRandomMode == RandomMode.LIMITED_RANDOM || currentRandomMode == RandomMode.EVEN_AMOUNTS) {
                        settings.add(
                            SettingItem.NumberSetting(
                                title = "Number of expansions to choose from",
                                number = currentRandomExpAmount,
                                min = 1,
                                max = 10,
                                onNumberChange = { setRandomExpansionAmount(it) }
                            )
                        )
                    }

                    settings.add(
                        SettingItem.SwitchSetting(
                            title = "Allow striking cards",
                            description = "Strike cards by swiping them away",
                            isChecked = currentAllowVetoing,
                            onCheckedChange = { setAllowVetoing(it); setNumberOfCardsToGenerate(10) }
                        )
                    )

                    if (currentAllowVetoing) {
                        settings.add(
                            SettingItem.ChoiceSetting(
                                title = "Veto mode",
                                selectedOption = currentVetoMode,
                                allOptions = VetoMode.entries.toList(),
                                optionDisplayFormatter = { it.displayName },
                                onOptionSelected = { setVetoMode(it); setNumberOfCardsToGenerate(10) },
                                description =
                                """Choose what happens when a card is vetoed.

Reroll from same: select cards from the same expansion as the vetoed card.

Reroll from any: select cards completely randomly from selected expansions.

Don't reroll: just remove cards until there's only 10 left."""
                            )
                        )

                        if (currentVetoMode == VetoMode.NO_REROLL) {
                            settings.add(
                                SettingItem.NumberSetting(
                                    title = "Number of cards to generate",
                                    number = currentNumCardsToGen,
                                    min = 10,
                                    max = 20,
                                    onNumberChange = { setNumberOfCardsToGenerate(it) }
                                )
                            )
                        }
                    }

                    // Landscapes Section
                    settings.add(SettingItem.SectionHeader("Landscape cards"))
                    settings.add(
                        SettingItem.NumberSetting(
                            title = "Landscape cards to include",
                            number = currentLandscapeCategories,
                            min = 0,
                            max = 2,
                            onNumberChange = { setLandscapeCategories(it) }
                        )
                    )

                    settings.add(
                        SettingItem.SwitchSetting(
                            title = "Use different landscape categories",
                            isChecked = currentLandscapeDiffCat,
                            onCheckedChange = { setLandscapeDifferentCategories(it) }
                        )
                    )

                    settings.add(
                        SettingItem.SwitchSetting(
                            title = "Pick landscapes from any owned expansion",
                            isChecked = currentPickLandscapesAny,
                            onCheckedChange = { setPickLandscapesFromAnyOwned(it) }
                        )
                    )

                    // Expansions Section
                    settings.add(SettingItem.SectionHeader("Dark Ages, Prosperity, Promo cards"))
                    settings.add(
                        SettingItem.ChoiceSetting(
                            title = "Dark Ages Shelters",
                            selectedOption = currentDarkAgesMode,
                            allOptions = DarkAgesMode.entries.toList(),
                            optionDisplayFormatter = { it.displayName },
                            onOptionSelected = { setDarkAgesStarterCardsMode(it) },
                            imageName = "set_dark_ages"
                        )
                    )
                    settings.add(
                        SettingItem.ChoiceSetting(
                            title = "Platinum and Colony",
                            selectedOption = currentProsperityMode,
                            allOptions = ProsperityMode.entries.toList(),
                            optionDisplayFormatter = { it.displayName },
                            onOptionSelected = { setProsperityBasicCardsMode(it) },
                            imageName = "set_prosperity_2e"
                        )
                    )
                    settings.add(
                        SettingItem.ChoiceSetting(
                            title = "Promo Cards",
                            selectedOption = currentPromoMode,
                            allOptions = PromoMode.entries.toList(),
                            optionDisplayFormatter = { it.displayName },
                            onOptionSelected = { setPromoMode(it) },
                            imageName = "set_promo"
                        )
                    )

                    // Hierarchical Rules Section
                    settings.add(SettingItem.SectionHeader("Card generation rules"))
                    settings.add(
                        SettingItem.NavigationSetting(
                            title = "Card types",
                            description = "Specify rules for Action, Treasure, Victory cards and more",
                            onClick = { navigateToSubScreen(SettingsSubScreen.CARD_TYPES) }
                        )
                    )
                    settings.add(
                        SettingItem.NavigationSetting(
                            title = "Card categories",
                            description = "Specify rules for Villages, Trashers, Draw cards and more",
                            onClick = { navigateToSubScreen(SettingsSubScreen.CARD_CATEGORIES) }
                        )
                    )
                    settings.add(
                        SettingItem.NavigationSetting(
                            title = "Card costs",
                            description = "Specify rules for cards with specific costs",
                            onClick = { navigateToSubScreen(SettingsSubScreen.CARD_COSTS) }
                        )
                    )
                    settings.add(
                        SettingItem.NavigationSetting(
                            title = "Landscape types",
                            description = "Specify rules for Events, Landmarks, Projects and more",
                            onClick = { navigateToSubScreen(SettingsSubScreen.LANDSCAPES) }
                        )
                    )

                    settings.add(
                        SettingItem.ActionSetting(
                            title = "Reset generation rules",
                            description = "Set all card and landscape type rules to default",
                            onClick = { setShowResetRulesDialog(true) }
                        )
                    )

                    // Feedback Section
                    settings.add(SettingItem.SectionHeader("Feedback"))
                    settings.add(
                        SettingItem.FeedbackSetting(
                            title = "Send feedback",
                            subtitle = "Share your ideas, report bugs, or request features",
                            onClick = { /* Email client opening is platform-specific */ }
                        )
                    )
                }

                SettingsSubScreen.CARD_TYPES -> {
                    settings.add(SettingItem.SectionHeader("Card types"))
                    addRulesToSettings(settings, CardRules.TYPE_RULES, currentActiveRules)
                }

                SettingsSubScreen.CARD_CATEGORIES -> {
                    settings.add(SettingItem.SectionHeader("Card categories"))
                    addRulesToSettings(settings, CardRules.CATEGORY_RULES, currentActiveRules)
                }

                SettingsSubScreen.CARD_COSTS -> {
                    settings.add(SettingItem.SectionHeader("Card costs"))
                    addRulesToSettings(settings, CardRules.COST_RULES, currentActiveRules)
                }

                SettingsSubScreen.LANDSCAPES -> {
                    settings.add(SettingItem.SectionHeader("Landscape types"))
                    addLandscapeRulesToSettings(settings, CardRules.LANDSCAPE_RULES, currentLandscapeRules)
                }
            }

            settings
        }
    }

    private fun addRulesToSettings(
        settings: MutableList<SettingItem>,
        rules: List<GenerationRule>,
        currentActiveRules: Map<String, RuleOption>
    ) {
        rules.forEach { rule ->
            val currentOption = currentActiveRules[rule.id] ?: RuleOption.ALLOW

            settings.add(
                SettingItem.RangeRuleSetting(
                    title = rule.name,
                    min = currentOption.min,
                    max = currentOption.max,
                    onRangeChange = { newMin, newMax ->
                        setRuleOption(rule.id, RuleOption(newMin, newMax))
                    },
                    imageName = rule.imageName
                )
            )
        }
    }

    private fun addLandscapeRulesToSettings(
        settings: MutableList<SettingItem>,
        rules: List<GenerationRule>,
        currentLandscapeRules: Map<String, Boolean>
    ) {
        rules.forEach { rule ->
            val isEnabled = currentLandscapeRules[rule.id] ?: true

            settings.add(
                SettingItem.SwitchSetting(
                    title = rule.name,
                    isChecked = isEnabled,
                    onCheckedChange = { enabled -> setLandscapeRule(rule.id, enabled) },
                    imageName = rule.imageName
                )
            )
        }
    }

    private fun navigateToSubScreen(subScreen: SettingsSubScreen) {
        _uiState.update { it.copy(currentSubScreen = subScreen) }
    }

    fun setDarkMode(isDarkMode: Boolean?) {
        launch { userPrefsRepository.setDarkMode(isDarkMode) }
    }

    fun setRandomMode(newMode: RandomMode) {
        launch { userPrefsRepository.setRandomMode(newMode) }
    }

    fun setRandomExpansionAmount(amount: Int) {
        launch { userPrefsRepository.setRandomExpansionAmount(amount) }
    }

    fun setVetoMode(newMode: VetoMode) {
        launch { userPrefsRepository.setVetoMode(newMode) }
    }

    fun setAllowVetoing(allow: Boolean) {
        launch { userPrefsRepository.setAllowVetoing(allow) }
    }

    fun setUseSystemTheme(useSystem: Boolean) {
        launch { userPrefsRepository.setUseSystemTheme(useSystem) }
    }

    fun setNumberOfCardsToGenerate(amount: Int) {
        launch { userPrefsRepository.setNumberOfCardsToGenerate(amount) }
    }

    fun setLandscapeCategories(amount: Int) {
        launch { userPrefsRepository.setLandscapeCount(amount) }
    }

    fun setLandscapeDifferentCategories(isDifferent: Boolean) {
        launch { userPrefsRepository.setLandscapeDifferentCategories(isDifferent) }
    }

    fun setPickLandscapesFromAnyOwned(pickAny: Boolean) {
        launch { userPrefsRepository.setPickLandscapesFromAnyOwned(pickAny) }
    }

    fun setDarkAgesStarterCardsMode(newMode: DarkAgesMode) {
        launch { userPrefsRepository.setDarkAgesStarterCardsMode(newMode) }
    }

    fun setProsperityBasicCardsMode(newMode: ProsperityMode) {
        launch { userPrefsRepository.setProsperityBasicCardsMode(newMode) }
    }

    fun setPromoMode(newMode: PromoMode) {
        launch { userPrefsRepository.setPromoMode(newMode) }
    }

    fun setRuleOption(ruleId: String, option: RuleOption) {
        launch { userPrefsRepository.setRuleOption(ruleId, option) }
    }

    fun setLandscapeRule(ruleId: String, enabled: Boolean) {
        launch { userPrefsRepository.setLandscapeRule(ruleId, enabled) }
    }

    fun setShowResetRulesDialog(show: Boolean) {
        _uiState.update { it.copy(showResetRulesDialog = show) }
    }

    fun resetGenerationRules() {
        launch {
            userPrefsRepository.resetGenerationRules()
            setShowResetRulesDialog(false)
        }
    }

    override fun handleBackNavigation(): Boolean {
        if (_uiState.value.currentSubScreen != SettingsSubScreen.MAIN) {
            _uiState.update { it.copy(currentSubScreen = SettingsSubScreen.MAIN) }
            return true
        }
        return false
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    override fun triggerScrollToTop() {
        if (_uiState.value.currentSubScreen == SettingsSubScreen.MAIN) {
            launch {
                _scrollToTopEvent.emit(Unit)
            }
        } else {
            _uiState.update { it.copy(currentSubScreen = SettingsSubScreen.MAIN) }
        }
    }

    override fun onSortTypeSelected(sortType: AppSortType) {
        // Stub - the Settings screen shows no sort icon
    }

    private val _currentAppSortType = MutableStateFlow<AppSortType?>(null)
    override val currentAppSortType: StateFlow<AppSortType?> = _currentAppSortType.asStateFlow()

    override val showBackButton: StateFlow<Boolean> = _uiState.map {
        it.currentSubScreen != SettingsSubScreen.MAIN
    }.stateIn(scope, SUBSCRIBED, false)

    override val showTopAppBar: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}
