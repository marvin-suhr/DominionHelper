package dev.msuhr.dominionkingdoms.model

object CardRules {
    val TYPE_RULES = listOf(
        GenerationRule("type_action", "Action") { it.types.contains(Type.ACTION) },
        GenerationRule("type_treasure", "Treasure") { it.types.contains(Type.TREASURE) },
        GenerationRule("type_victory", "Victory") { it.types.contains(Type.VICTORY) },
        GenerationRule("type_attack", "Attack") { it.types.contains(Type.ATTACK) },
        GenerationRule("type_duration", "Duration") { it.types.contains(Type.DURATION) },
        GenerationRule("type_reaction", "Reaction") { it.types.contains(Type.REACTION) },
        GenerationRule("type_night", "Night") { it.types.contains(Type.NIGHT) },
        GenerationRule("type_command", "Command") { it.types.contains(Type.COMMAND) },
        GenerationRule("type_looter", "Looter") { it.types.contains(Type.LOOTER) },
        GenerationRule("type_reserve", "Reserve") { it.types.contains(Type.RESERVE) },
        GenerationRule("type_gathering", "Gathering") { it.types.contains(Type.GATHERING) },
        GenerationRule("type_doom", "Doom") { it.types.contains(Type.DOOM) },
        GenerationRule("type_fate", "Fate") { it.types.contains(Type.FATE) },
        GenerationRule("type_liaison", "Liaison") { it.types.contains(Type.LIAISON) },
        GenerationRule("type_omen", "Omen") { it.types.contains(Type.OMEN) },
        GenerationRule("type_shadow", "Shadow") { it.types.contains(Type.SHADOW) },
    )

    val COST_RULES = listOf(
        GenerationRule("cost_2", "Cost 2") { it.cost == 2 },
        GenerationRule("cost_3", "Cost 3") { it.cost == 3 },
        GenerationRule("cost_4", "Cost 4") { it.cost == 4 },
        GenerationRule("cost_5", "Cost 5") { it.cost == 5 },
        GenerationRule("cost_6_plus", "Cost 6+") { (it.cost ?: 0) >= 6 },
        GenerationRule("cost_debt", "Debt") { it.debt > 0 },
        GenerationRule("cost_potion", "Potion") { it.potion },
        GenerationRule("cost_overpay", "Overpay") { it.overpay }
    )

    val CATEGORY_RULES = listOf(
        GenerationRule("cat_village", "Village") { it.categories.contains(Category.VILLAGE) },
        GenerationRule("cat_trasher", "Trasher") { it.categories.contains(Category.TRASHER) },
        GenerationRule("cat_cantrip", "Cantrip") { it.categories.contains(Category.CANTRIP) },
        GenerationRule("cat_gainer", "Gainer") { it.categories.contains(Category.GAINER) },
        GenerationRule("cat_plus_buy", "Plus Buy") { it.categories.contains(Category.PLUSBUY) },
        GenerationRule("cat_draw", "Draw") { 
            it.categories.contains(Category.TERMINAL_DRAW) || 
            it.categories.contains(Category.NONTERMINAL_DRAW) ||
            it.categories.contains(Category.DRAW_TO_X)
        }
    )

    val LANDSCAPE_RULES = listOf(
        GenerationRule("landscape_event", "Event") { it.types.contains(Type.EVENT) },
        GenerationRule("landscape_landmark", "Landmark") { it.types.contains(Type.LANDMARK) },
        GenerationRule("landscape_artifact", "Artifact") { it.types.contains(Type.ARTIFACT) },
        GenerationRule("landscape_project", "Project") { it.types.contains(Type.PROJECT) },
        GenerationRule("landscape_way", "Way") { it.types.contains(Type.WAY) },
        GenerationRule("landscape_ally", "Ally") { it.types.contains(Type.ALLY) },
        GenerationRule("landscape_trait", "Trait") { it.types.contains(Type.TRAIT) },
        GenerationRule("landscape_prophecy", "Prophecy") { it.types.contains(Type.PROPHECY) },
    )

    val LANDSCAPE_TYPES = setOf(
        Type.EVENT, Type.LANDMARK, Type.ARTIFACT, Type.PROJECT,
        Type.WAY, Type.ALLY, Type.TRAIT, Type.PROPHECY
    )

    val ALL_RULES = TYPE_RULES + COST_RULES + CATEGORY_RULES + LANDSCAPE_RULES

    fun getRuleById(id: String): GenerationRule? = ALL_RULES.find { it.id == id }
}
