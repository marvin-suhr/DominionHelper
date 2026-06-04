package dev.msuhr.dominionkingdoms.model

object CardRules {
    val TYPE_RULES = listOf(
        GenerationRule("type_action", "Action", imageName = "type_action") { it.types.contains(Type.ACTION) },
        GenerationRule("type_treasure", "Treasure", imageName = "type_treasure") { it.types.contains(Type.TREASURE) },
        GenerationRule("type_victory", "Victory", imageName = "type_victory") { it.types.contains(Type.VICTORY) },
        GenerationRule("type_attack", "Attack", imageName = "type_attack") { it.types.contains(Type.ATTACK) },
        GenerationRule("type_duration", "Duration", imageName = "type_duration") { it.types.contains(Type.DURATION) },
        GenerationRule("type_reaction", "Reaction", imageName = "type_reaction") { it.types.contains(Type.REACTION) },
        GenerationRule("type_night", "Night", imageName = "type_night") { it.types.contains(Type.NIGHT) },
        GenerationRule("type_command", "Command", imageName = "type_command") { it.types.contains(Type.COMMAND) },
        GenerationRule("type_looter", "Looter", imageName = "type_looter") { it.types.contains(Type.LOOTER) },
        GenerationRule("type_reserve", "Reserve", imageName = "type_reserve") { it.types.contains(Type.RESERVE) },
        GenerationRule("type_gathering", "Gathering", imageName = "type_gathering") { it.types.contains(Type.GATHERING) },
        GenerationRule("type_doom", "Doom", imageName = "type_doom") { it.types.contains(Type.DOOM) },
        GenerationRule("type_fate", "Fate", imageName = "type_fate") { it.types.contains(Type.FATE) },
        GenerationRule("type_liaison", "Liaison", imageName = "type_liaison") { it.types.contains(Type.LIAISON) },
        GenerationRule("type_omen", "Omen", imageName = "type_omen") { it.types.contains(Type.OMEN) },
        GenerationRule("type_shadow", "Shadow", imageName = "type_shadow") { it.types.contains(Type.SHADOW) },
    )

    val COST_RULES = listOf(
        GenerationRule("cost_2", "Cost 2", imageName = "cost_2") { it.cost == 2 },
        GenerationRule("cost_3", "Cost 3", imageName = "cost_3") { it.cost == 3 },
        GenerationRule("cost_4", "Cost 4", imageName = "cost_4") { it.cost == 4 },
        GenerationRule("cost_5", "Cost 5", imageName = "cost_5") { it.cost == 5 },
        GenerationRule("cost_6_plus", "Cost 6+", imageName = "cost_6") { (it.cost ?: 0) >= 6 },
        GenerationRule("cost_debt", "Debt", imageName = "type_victory") { it.debt > 0 },
        GenerationRule("cost_potion", "Potion", imageName = "set_alchemy") { it.potion },
        GenerationRule("cost_overpay", "Overpay", imageName = "cost_plus") { it.overpay }
    )

    val CATEGORY_RULES = listOf(
        GenerationRule("cat_village", "Village", imageName = "cat_village") { it.categories.contains(Category.VILLAGE) },
        GenerationRule("cat_trasher", "Trasher", imageName = "cat_trasher") { it.categories.contains(Category.TRASHER) },
        GenerationRule("cat_cantrip", "Cantrip", imageName = "cat_cantrip") { it.categories.contains(Category.CANTRIP) },
        GenerationRule("cat_gainer", "Gainer", imageName = "cat_gainer") { it.categories.contains(Category.GAINER) },
        GenerationRule("cat_plus_buy", "Plus Buy", imageName = "cat_plusbuy") { it.categories.contains(Category.PLUSBUY) },
        GenerationRule("cat_terminal_draw", "Terminal draw", imageName = "cat_terminal_draw") { it.categories.contains(Category.TERMINAL_DRAW) } ,
        GenerationRule("cat_non_terminal_draw", "Non-terminal draw", imageName = "cat_non_terminal_draw") { it.categories.contains(Category.NONTERMINAL_DRAW) },
        GenerationRule("cat_draw_to_X", "Draw to X", imageName = "cat_draw_to_x") { it.categories.contains(Category.DRAW_TO_X) },
        GenerationRule("cat_curser", "Curser", imageName = "cat_curser") { it.categories.contains(Category.CURSER) },
        GenerationRule("cat_non_terminal", "Non-terminal", imageName = "cat_non_terminal") { it.categories.contains(Category.NONTERMINAL) },
        GenerationRule("cat_terminal", "Terminal", imageName = "cat_terminal") { it.categories.contains(Category.TERMINAL) },
        GenerationRule("cat_throneroom_variant", "Throneroom Variant", imageName = "cat_throneroom_variant") { it.categories.contains(Category.THRONEROOM_VARIANT) },
        GenerationRule("cat_alt_vp", "Alt VP", imageName = "cat_alt_vp") { it.categories.contains(Category.ALT_VP) },
        GenerationRule("cat_deck_inspector", "Deck Inspector", imageName = "cat_deck_inspector") { it.categories.contains(Category.DECK_INSPECTOR) },
        GenerationRule("cat_trash_for_benefit", "Trash for benefit", imageName = "cat_trash_for_benefit") { it.categories.contains(Category.TRASH_FOR_BENEFIT) },
        GenerationRule("cat_handsize_attack", "Handsize Attack", imageName = "cat_handsize_attack") { it.categories.contains(Category.HANDSIZE_ATTACK) },
        GenerationRule("cat_junker", "Junker", imageName = "cat_junker") { it.categories.contains(Category.JUNKER) },
        GenerationRule("cat_deck_order_attack", "Deck order attack", imageName = "cat_deck_order_attack") { it.categories.contains(Category.DECK_ORDER_ATTACK) },
        GenerationRule("cat_trashing_attack", "Trashing attack", imageName = "cat_trashing_attack") { it.categories.contains(Category.TRASHING_ATTACK) },
        GenerationRule("cat_peddler_variant", "Peddler variant", imageName = "cat_peddler_variant") { it.categories.contains(Category.PEDDLER_VARIANT) },
        GenerationRule("cat_terminal_silver", "Terminal silver", imageName = "cat_terminal_silver") { it.categories.contains(Category.TERMINAL_SILVER) },
        GenerationRule("cat_sifter", "Sifter", imageName = "cat_sifter") { it.categories.contains(Category.SIFTER) },
        GenerationRule("cat_coffers", "Coffers", imageName = "") { it.categories.contains(Category.COFFERS) },
        GenerationRule("cat_villagers", "Villagers", imageName = "") { it.categories.contains(Category.VILLAGERS) },
        GenerationRule("cat_exile", "Exile", imageName = "") { it.categories.contains(Category.EXILE) },
    )

    val LANDSCAPE_RULES = listOf(
        GenerationRule("landscape_event", "Event", target = RuleTarget.LANDSCAPE, imageName = "set_adventures") { it.types.contains(Type.EVENT) },
        GenerationRule("landscape_landmark", "Landmark", target = RuleTarget.LANDSCAPE, imageName = "set_empires") { it.types.contains(Type.LANDMARK) },
        GenerationRule("landscape_project", "Project", target = RuleTarget.LANDSCAPE, imageName = "set_renaissance") { it.types.contains(Type.PROJECT) },
        GenerationRule("landscape_trait", "Trait", target = RuleTarget.LANDSCAPE, imageName = "set_plunder") { it.types.contains(Type.TRAIT) },
        GenerationRule("landscape_way", "Way", target = RuleTarget.LANDSCAPE, imageName = "set_menagerie") { it.types.contains(Type.WAY) },
    )

    val LANDSCAPE_TYPES = setOf(
        Type.EVENT, Type.LANDMARK, Type.ARTIFACT, Type.PROJECT,
        Type.WAY, Type.ALLY, Type.TRAIT, Type.PROPHECY
    )

    val LANDSCAPE_RULE_TO_TYPE = mapOf(
        "landscape_event" to Type.EVENT,
        "landscape_landmark" to Type.LANDMARK,
        "landscape_project" to Type.PROJECT,
        "landscape_trait" to Type.TRAIT,
        "landscape_way" to Type.WAY
    )

    val ALL_RULES = TYPE_RULES + COST_RULES + CATEGORY_RULES + LANDSCAPE_RULES

    fun getRuleById(id: String): GenerationRule? = ALL_RULES.find { it.id == id }
}
