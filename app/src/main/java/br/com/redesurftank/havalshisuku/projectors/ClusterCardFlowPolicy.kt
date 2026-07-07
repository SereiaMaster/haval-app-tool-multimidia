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
        // Card 0 = cluster nativo original do carro. Para o usuário poder "passar para o
        // lado" e ver o cluster de fábrica, escondemos o overlay nesse card (revelando o
        // nativo por baixo) — igual ao master, que faz isso de forma incondicional
        // (circularView.isVisible = card != 0).
        //
        // NAO bloqueamos por aviso: o cluster NATIVO exibe os proprios avisos do carro, entao
        // esconder o overlay no card 0 durante um aviso e seguro (e o upstream so bloqueava
        // aqui "para preservar o tema", que e justamente o que impedia de chegar no nativo).
        // Mantemos o bloqueio por projecao AA/CarPlay/espelhamento para nao interromper uma
        // sessao de projecao ativa.
        if (cardId != ClusterCardIds.NATIVE_CARD) return false
        if (projectionActive) return false
        return true
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
