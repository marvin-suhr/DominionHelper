package dev.msuhr.dominionkingdoms.ui

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.msuhr.dominionkingdoms.data.UserPrefsRepository
import dev.msuhr.dominionkingdoms.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.* // TODO When does this wildcard make sense?
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingItem {
    data class SectionHeader(val title: String) : SettingItem() {
        override fun toString(): String = "SectionHeader(title='$title')"
    }

    data class SwitchSetting(
        val title: String,
        val description: String? = null,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val imageName: String = ""
    ) : SettingItem() {
        override fun toString(): String = "SwitchSetting(title='$title', isChecked=$isChecked)"
    }

    data class TextSetting(
        val title: String,
        val text: String,
        val onTextChange: (String) -> Unit
    ) : SettingItem() {
        override fun toString(): String = "TextSetting(title='$title', text='$text')"
    }

    data class NumberSetting(
        val title: String,
        val number: Int,
        val min: Int,
        val max: Int,
        val onNumberChange: (Int) -> Unit
    ) : SettingItem() {
        override fun toString(): String = "NumberSetting(title='$title', number=$number)"
    }

    data class ChoiceSetting<E : Enum<E>>(
        val title: String,
        val selectedOption: E,
        val allOptions: List<E>,
        val optionDisplayFormatter: (E) -> String,
        val onOptionSelected: (E) -> Unit,
        val description: String? = null, // Optional description for info button
        val imageName: String = "" // Optional leading icon name
    ) : SettingItem() {
        override fun toString(): String = "ChoiceSetting(title='$title', selectedOption=$selectedOption)"
    }

    data class FeedbackSetting(
        val title: String,
        val subtitle: String,
        val onClick: () -> Unit
    ) : SettingItem() {
        override fun toString(): String = "FeedbackSetting(title='$title', subtitle='$subtitle')"
    }

    data class NavigationSetting(
        val title: String,
        val description: String? = null,
        val onClick: () -> Unit
    ) : SettingItem() {
        override fun toString(): String = "NavigationSetting(title='$title')"
    }

    data class RangeRuleSetting(
        val title: String,
        val min: Int,
        val max: Int,
        val onRangeChange: (Int, Int) -> Unit,
        val imageName: String = ""
    ) : SettingItem() {
        override fun toString(): String = "RangeRuleSetting(title='$title', min=$min, max=$max)"
    }
}

enum class RandomMode(val displayName: String) {
    FULL_RANDOM("Full Random"),
    LIMITED_RANDOM("Limited Random"),
    EVEN_AMOUNTS("Even Amounts")
}

enum class VetoMode(val displayName: String) {
    REROLL_SAME("Reroll from the same expansion"),
    REROLL_ANY("Reroll from any selected expansion"),
    NO_REROLL("Don't reroll (10 cards minimum)")
}

enum class DarkAgesMode(val displayName: String) {
    TEN_PERCENT_PER_CARD("10% per Dark Ages card"),
    IF_PRESENT("When at least one card is present"),
    NEVER("Never")
}

enum class ProsperityMode(val displayName: String) {
    TEN_PERCENT_PER_CARD("10% per Prosperity card"),
    IF_PRESENT("When at least one card is present"),
    NEVER("Never")
    // ALWAYS_IF_PROSPERITY_OWNED ??
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel(), ScreenViewModel {

    data class SettingsUiState(
        val settings: List<SettingItem> = emptyList(),
        val currentSubScreen: SettingsSubScreen = SettingsSubScreen.MAIN
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        getSettings()
            .onEach { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
            .launchIn(viewModelScope) // TODO wow this is nifty
    }

    // TODO: ATROCIOUS. Make this more readable
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
            userPrefsRepository.activeRules,
            userPrefsRepository.landscapeRules,
            _uiState.map { it.currentSubScreen }.distinctUntilChanged()
        ) { values ->
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
            val currentActiveRules = values[12] as Map<String, RuleOption>
            val currentLandscapeRules = values[13] as Map<String, Boolean>
            val currentSubScreen = values[14] as SettingsSubScreen

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
                    // Only show "Use system theme" on Android 12+ (API 31+)
                    // where dynamic colors are available
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        settings.add(
                            SettingItem.SwitchSetting(
                                title = "Dynamic color",
                                description = "Use colors from system style",
                                isChecked = useSystemTheme,
                                onCheckedChange = { setUseSystemTheme(it) }
                            )
                        )
                    }

                    // Generation Section
                    settings.add(SettingItem.SectionHeader("Kingdom generation"))

                    settings.add(
                        SettingItem.NumberSetting(
                            title = "Number of cards to generate",
                            number = currentNumCardsToGen,
                            min = 10,
                            max = 20,
                            onNumberChange = { setNumberOfCardsToGenerate(it) }
                        )
                    )

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

                    // Switch to allow or disallow vetoing cards
                    settings.add(
                        SettingItem.SwitchSetting(
                            title = "Allow vetoing cards",
                            description = "Allow striking cards after generating",
                            isChecked = currentAllowVetoing,
                            onCheckedChange = { setAllowVetoing(it) }
                        )
                    )

                    if (currentAllowVetoing) {
                        settings.add(
                            SettingItem.ChoiceSetting(
                                title = "Veto mode",
                                selectedOption = currentVetoMode,
                                allOptions = VetoMode.entries.toList(),
                                optionDisplayFormatter = { it.displayName },
                                onOptionSelected = { setVetoMode(it) },
                                description =
                                """Choose what happens when a card is vetoed.

Reroll from same: select cards from the same expansion as the vetoed card.

Reroll from any: select cards completely randomly from selected expansions.

Don't reroll: just remove cards until there's only 10 left."""
                            )
                        )
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
                    settings.add(SettingItem.SectionHeader("Dark Ages and Prosperity cards"))
                    settings.add(
                        SettingItem.ChoiceSetting(
                            title = "Dark Ages Shelters",
                            selectedOption = currentDarkAgesMode,
                            allOptions = DarkAgesMode.entries.toList(),
                            optionDisplayFormatter = { it.displayName },
                            onOptionSelected = { setDarkAgesStarterCardsMode(it) }
                        )
                    )
                    settings.add(
                        SettingItem.ChoiceSetting(
                            title = "Platinum and Colony",
                            selectedOption = currentProsperityMode,
                            allOptions = ProsperityMode.entries.toList(),
                            optionDisplayFormatter = { it.displayName },
                            onOptionSelected = { setProsperityBasicCardsMode(it) }
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

                    // Feedback Section
                    settings.add(SettingItem.SectionHeader("Feedback"))
                    settings.add(
                        SettingItem.FeedbackSetting(
                            title = "Send feedback",
                            subtitle = "Share your ideas, report bugs, or request features",
                            onClick = { /* Open email client - handled in UI */ } // TODO - why?
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
        rules: List<GenerationRule>, // TODO i dont think we need the rules here
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
        viewModelScope.launch {
            userPrefsRepository.setDarkMode(isDarkMode)
        }
    }

    fun setRandomMode(newMode: RandomMode) {
        viewModelScope.launch {
            userPrefsRepository.setRandomMode(newMode)
        }
    }

    fun setRandomExpansionAmount(amount: Int) {
        viewModelScope.launch {
            userPrefsRepository.setRandomExpansionAmount(amount)
        }
    }

    fun setVetoMode(newMode: VetoMode) {
        viewModelScope.launch {
            userPrefsRepository.setVetoMode(newMode)
        }
    }

    fun setAllowVetoing(allow: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setAllowVetoing(allow)
        }
    }

    fun setUseSystemTheme(useSystem: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setUseSystemTheme(useSystem)
        }
    }

    fun setNumberOfCardsToGenerate(amount: Int) {
        viewModelScope.launch {
            userPrefsRepository.setNumberOfCardsToGenerate(amount)
        }
    }

    fun setLandscapeCategories(amount: Int) {
        viewModelScope.launch {
            userPrefsRepository.setLandscapeCount(amount)
        }
    }

    fun setLandscapeDifferentCategories(isDifferent: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setLandscapeDifferentCategories(isDifferent)
        }
    }

    fun setPickLandscapesFromAnyOwned(pickAny: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setPickLandscapesFromAnyOwned(pickAny)
        }
    }

    fun setDarkAgesStarterCardsMode(newMode: DarkAgesMode) {
        viewModelScope.launch {
            userPrefsRepository.setDarkAgesStarterCardsMode(newMode)
        }
    }

    fun setProsperityBasicCardsMode(newMode: ProsperityMode) {
        viewModelScope.launch {
            userPrefsRepository.setProsperityBasicCardsMode(newMode)
        }
    }

    fun setRuleOption(ruleId: String, option: RuleOption) {
        viewModelScope.launch {
            userPrefsRepository.setRuleOption(ruleId, option)
        }
    }

    fun setLandscapeRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setLandscapeRule(ruleId, enabled)
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
        viewModelScope.launch {
            _scrollToTopEvent.emit(Unit)
        }
    }

    override fun onSortTypeSelected(sortType: AppSortType) {
        // Stub - TODO remove icon
    }

    private val _currentAppSortType = MutableStateFlow<AppSortType?>(null)
    override val currentAppSortType: StateFlow<AppSortType?> = _currentAppSortType.asStateFlow()

    override val showBackButton: StateFlow<Boolean> = _uiState.map { 
        it.currentSubScreen != SettingsSubScreen.MAIN 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    override val showTopAppBar: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}
