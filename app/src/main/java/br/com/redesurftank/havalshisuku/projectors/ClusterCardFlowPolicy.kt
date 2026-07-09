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
        // Card 0 is the car's own navigable cluster region. Drop our Presentation
        // entirely here so the native cluster (or the projection app on D3) shows
        // through with zero paint from us. This is unconditional: projection state
        // and warnings do not matter on card 0 — the user explicitly wants the
        // circular region 100% released whenever card 0 is active.
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
