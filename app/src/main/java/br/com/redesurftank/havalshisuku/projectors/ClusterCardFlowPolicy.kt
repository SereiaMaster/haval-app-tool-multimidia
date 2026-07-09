package br.com.redesurftank.havalshisuku.projectors

internal object ClusterCardFlowPolicy {
    data class Decision(
            val pushProjectionStateBeforeCard: Boolean,
            val forceProjectionStateBeforeCard: Boolean,
            val updateVirtualClusterVisibility: Boolean,
            val clearAppliedAppConfigCache: Boolean,
            val syncSecondaryDisplayApps: Boolean,
            val syncVisibleCardValues: Boolean
    )

    fun cardCanAffectManagedAppBounds(previousCard: Int, nextCard: Int): Boolean {
        if (previousCard == nextCard) return false
        return previousCard == ClusterCardIds.NATIVE_CARD || nextCard == ClusterCardIds.NATIVE_CARD
    }

    fun isCardBackedMenu(cardId: Int): Boolean {
        return cardId == ClusterCardIds.MAIN_MENU_CARD ||
                cardId == ClusterCardIds.AIRCON_CARD
    }

    fun shouldUseNativeCardPassThrough(
            cardId: Int,
            warningActive: Boolean,
            projectionActive: Boolean
    ): Boolean {
        // No card nativo (0) o usuario desativou o cluster projetado (toque longo em
        // voltar) e quer o cluster original do carro de volta. Nesse card o overlay
        // deve ser interrompido 100%, INDEPENDENTE de qualquer condicao (projecao no
        // D3 ou aviso ativo): o proprio cluster nativo lida com seus avisos e a
        // projecao ja ocupa o display. O tema sobreposto so faz sentido nos cards
        // projetados (1/3), que nunca passam direto por aqui.
        return cardId == ClusterCardIds.NATIVE_CARD
    }

    fun decideCardChange(
            nextCard: Int,
            projectionActive: Boolean,
            projectionStateMayBeStale: Boolean,
            hasManagedSecondaryDisplayWork: Boolean,
            cardCanAffectManagedAppBounds: Boolean
    ): Decision {
        val projectionWork = projectionActive || projectionStateMayBeStale
        val managedBoundsWork = hasManagedSecondaryDisplayWork && cardCanAffectManagedAppBounds

        return Decision(
                pushProjectionStateBeforeCard = projectionActive || projectionStateMayBeStale,
                forceProjectionStateBeforeCard = projectionActive,
                updateVirtualClusterVisibility = projectionWork || managedBoundsWork,
                clearAppliedAppConfigCache = managedBoundsWork,
                syncSecondaryDisplayApps = managedBoundsWork,
                syncVisibleCardValues = isCardBackedMenu(nextCard)
        )
    }
}
