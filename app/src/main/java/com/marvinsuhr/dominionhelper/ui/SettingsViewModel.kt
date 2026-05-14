package com.marvinsuhr.dominionhelper.ui

import android.os.Build
import com.marvinsuhr.dominionhelper.data.UserPrefsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marvinsuhr.dominionhelper.model.AppSortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingItem {
    data class SectionHeader(
        val title: String
    ) : SettingItem() {
        override fun toString(): String {
            return "SectionHeader(title='$title')"
        }
    }

    data class SwitchSetting(
        //val key: String,
        val title: String,
        val description: String? = null,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingItem() {
        override fun toString(): String {
            return "SwitchSetting(title='$title', isChecked=$isChecked)"
        }
    }

    data class TextSetting(
        val title: String,
        val text: String,
        val onTextChange: (String) -> Unit
    ) : SettingItem() {
        override fun toString(): String {
            return "TextSetting(title='$title', text=$text)"
        }
    }

    data class NumberSetting(
        val title: String,
        val number: Int,
        val min: Int,
        val max: Int,
        val onNumberChange: (Int) -> Unit
    ) : SettingItem() {
        override fun toString(): String {
            return "NumberSetting(title='$title', number=$number, min=$min, max=$max)"
        }
    }

    data class ChoiceSetting<E : Enum<E>>(
        val title: String,
        val selectedOption: E,
        val allOptions: List<E>,
        val optionDisplayFormatter: (E) -> String = { it.name }, // Default display is enum constant name
        val onOptionSelected: (E) -> Unit,
        val description: String? = null // Optional description for info button
    ) : SettingItem() {
        override fun toString(): String {
            return "ChoiceSetting(title='$title', selectedOption=$selectedOption)"
        }
    }

    data class FeedbackSetting(
        val title: String,
        val subtitle: String,
        val onClick: () -> Unit
    ) : SettingItem() {
        override fun toString(): String {
            return "FeedbackSetting(title='$title')"
        }
    }
}

enum class RandomMode(val displayName: String) {
    FULL_RANDOM("Full Random"),
    EVEN_AMOUNTS("Even Amounts"),

    // TODO does this make sense?
    //X_OF_EACH_SET("X from Each Set")
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
    IF_PRESENT("When at least 1 card is present"),
    NEVER("Never")
    // ALWAYS_IF_PROSPERITY_OWNED ??
}

enum class DarkModeSetting(val displayName: String) {
    SYSTEM("System default"),
    DARK("Dark"),
    LIGHT("Light")
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel(), ScreenViewModel {

    data class SettingsUiState(
        val settings: List<SettingItem> = emptyList()
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSettings().collect { updatedSettings ->
                _uiState.update {
                    it.copy(settings = updatedSettings)
                }
            }
        }
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
            userPrefsRepository.landscapeCategories,
            userPrefsRepository.landscapeDifferentCategories,
            userPrefsRepository.darkAgesStarterCardsMode,
            userPrefsRepository.prosperityBasicCardsMode
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
            val currentDarkAgesMode = values[9] as DarkAgesMode
            val currentProsperityMode = values[10] as ProsperityMode

            listOfNotNull( // Use listOfNotNull if some settings might be conditionally absent
                // Interface Section
                SettingItem.SectionHeader("Interface"),
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
                ),
                // Only show "Use system theme" on Android 12+ (API 31+)
                // where dynamic colors are available
                SettingItem.SwitchSetting(
                    title = "Dynamic color",
                    description = "Use colors from the system style",
                    isChecked = useSystemTheme,
                    onCheckedChange = { setUseSystemTheme(it) }
                ).takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },

                // Generation Section
                SettingItem.SectionHeader("Kingdom generation"),
                SettingItem.NumberSetting(
                    title = "Number of expansions to choose from",
                    number = currentRandomExpAmount,
                    min = 1,
                    max = 10,
                    onNumberChange = { setRandomExpansionAmount(it) }
                ),
                SettingItem.ChoiceSetting(
                    title = "Random mode",
                    selectedOption = currentRandomMode,
                    allOptions = RandomMode.entries.toList(),
                    optionDisplayFormatter = { it.displayName },
                    onOptionSelected = { setRandomMode(it) },
                    description =
"""Choose how cards are selected.
                        
Full Random: select cards completely randomly from selected expansions.
                        
Even Amounts: select equal card amounts from selected expansion."""
                ),

                // Switch to allow or disallow vetoing cards
                SettingItem.SwitchSetting(
                    title = "Allow vetoing cards",
                    description = "Allow striking cards after generating",
                    isChecked = currentAllowVetoing,
                    onCheckedChange = { setAllowVetoing(it) }
                ),

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
                ).takeIf { currentAllowVetoing },

                SettingItem.NumberSetting(
                    title = "Number of cards to generate",
                    number = currentNumCardsToGen,
                    min = 10,
                    max = 20,
                    onNumberChange = { setNumberOfCardsToGenerate(it) }
                ).takeIf { currentAllowVetoing },

                // Landscapes Section
                SettingItem.SectionHeader("Landscape cards"),
                SettingItem.NumberSetting(
                    title = "Landscape cards to include",
                    number = currentLandscapeCategories,
                    min = 0,
                    max = 2,
                    onNumberChange = { setLandscapeCategories(it) }
                ),

                SettingItem.SwitchSetting(
                    title = "Use different landscape categories",
                    isChecked = currentLandscapeDiffCat,
                    onCheckedChange = { setLandscapeDifferentCategories(it) }
                ),

                // Expansions Section
                SettingItem.SectionHeader("Dark Ages and Prosperity cards"),
                SettingItem.ChoiceSetting(
                    title = "Dark Ages starter cards",
                    selectedOption = currentDarkAgesMode,
                    allOptions = DarkAgesMode.entries.toList(),
                    optionDisplayFormatter = { it.displayName },
                    onOptionSelected = { setDarkAgesStarterCardsMode(it) }
                ),
                SettingItem.ChoiceSetting(
                    title = "Platinum and Colony",
                    selectedOption = currentProsperityMode,
                    allOptions = ProsperityMode.entries.toList(),
                    optionDisplayFormatter = { it.displayName },
                    onOptionSelected = { setProsperityBasicCardsMode(it) }
                ),

                // Feedback Section
                SettingItem.SectionHeader("Feedback"),
                SettingItem.FeedbackSetting(
                    title = "Send feedback",
                    subtitle = "Share your ideas, report bugs, or request features",
                    onClick = { /* Open email client - handled in UI */ }
                )
            )
        }
    }

    fun setDarkMode(isDarkMode: Boolean?) {
        viewModelScope.launch {
            userPrefsRepository.setDarkMode(isDarkMode)
        }
    }

    fun setRandomMode(mode: RandomMode) {
        viewModelScope.launch {
            userPrefsRepository.setRandomMode(mode)
        }
    }

    fun setRandomExpansionAmount(amount: Int) {
        viewModelScope.launch {
            userPrefsRepository.setRandomExpansionAmount(amount)
        }
    }

    fun setVetoMode(mode: VetoMode) {
        viewModelScope.launch {
            userPrefsRepository.setVetoMode(mode)
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
            userPrefsRepository.setLandscapeCategories(amount)
        }
    }

    fun setLandscapeDifferentCategories(isDifferent: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setLandscapeDifferentCategories(isDifferent)
        }
    }

    fun setDarkAgesStarterCardsMode(mode: DarkAgesMode) {
        viewModelScope.launch {
            userPrefsRepository.setDarkAgesStarterCardsMode(mode)
        }
    }

    fun setProsperityBasicCardsMode(mode: ProsperityMode) {
        viewModelScope.launch {
            userPrefsRepository.setProsperityBasicCardsMode(mode)
        }
    }

    override fun handleBackNavigation(): Boolean {
        return false
    }

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    override fun triggerScrollToTop() {
        _scrollToTopEvent.tryEmit(Unit)
    }

    override fun onSortTypeSelected(sortType: AppSortType) {
        // Stub
    }

    override val currentAppSortType: StateFlow<AppSortType?> = MutableStateFlow(null).asStateFlow()

    override val showBackButton: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val showTopAppBar: StateFlow<Boolean> = MutableStateFlow(false)
}
