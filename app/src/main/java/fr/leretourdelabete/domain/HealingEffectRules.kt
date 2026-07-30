package fr.leretourdelabete.domain

import fr.leretourdelabete.model.HealingOutcome

object HealingEffectRules {
    fun isGhoulOptionAvailable(round: Int): Boolean = round > 1

    fun text(outcome: HealingOutcome?): String = when (outcome) {
        HealingOutcome.VILLAGER ->
            "La guérison n'a eu aucun effet, ce villageois s'est révélé tout à fait normal.\n" +
                "Le joueur garde sa pierre bleue et la partie continue."
        HealingOutcome.GHOUL ->
            "La guérison a révélé que ce villageois était une goule. Nous avons tenté de " +
                "guérir son âme mais rien n'est certain.\n" +
                "Le joueur repose sa pierre jaune ou verte dans le sac de guérison, secoue " +
                "le sac pour mélanger les pierres puis tire au hasard une nouvelle pierre. " +
                "Il redevient villageois ou reste goule."
        HealingOutcome.WEREWOLF ->
            "La guérison a révélé que ce villageois était un loup-garou. Le rituel est " +
                "parvenu à guérir son corps mais peut-être pas son âme.\n" +
                "Le joueur repose sa pierre violette dans la boîte des loups puis tire au " +
                "hasard une nouvelle pierre du sac de guérison. Il redevient villageois ou " +
                "devient goule."
        null -> "Indiquez le rôle révélé pendant le rituel de guérison."
    }
}
