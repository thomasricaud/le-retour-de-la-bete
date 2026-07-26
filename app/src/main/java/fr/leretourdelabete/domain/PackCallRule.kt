package fr.leretourdelabete.domain

object PackCallRule {
    /**
     * Nombre maximal de villageois restants pour que l'appel de la meute soit juste.
     *
     * Pour un nombre positif de joueurs, ajouter 2 avant la division entière par 4
     * revient à arrondir N / 4 au plus proche, avec les demi-unités arrondies vers
     * le haut.
     */
    fun maxRemainingVillagers(playerCount: Int): Int {
        require(playerCount > 0) {
            "Le nombre de joueurs doit être strictement positif."
        }
        return (playerCount + 2) / 4
    }
}
