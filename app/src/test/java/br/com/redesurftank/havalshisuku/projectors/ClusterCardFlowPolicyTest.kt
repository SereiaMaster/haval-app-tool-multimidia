package br.com.redesurftank.havalshisuku.projectors

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterCardFlowPolicyTest {
    @Test
    fun normalAcMainMenuCardChangeUsesFastPathWithoutDisplaySync() {
        val decision =
                ClusterCardFlowPolicy.decideCardChange(
                        nextCard = ClusterCardIds.AIRCON_CARD,
                        projectionActive = false,
                        projectionStateMayBeStale = false,
                        hasManagedSecondaryDisplayWork = false,
                        cardCanAffectManagedAppBounds = false
                )

        assertFalse(decision.pushProjectionStateBeforeCard)
        assertFalse(decision.updateVirtualClusterVisibility)
        assertFalse(decision.clearAppliedAppConfigCache)
        assertFalse(decision.syncSecondaryDisplayApps)
        assertTrue(decision.syncVisibleCardValues)
    }

    @Test
    fun activeProjectionKeepsProtectedOrderingBeforeCardId() {
        val decision =
                ClusterCardFlowPolicy.decideCardChange(
                        nextCard = ClusterCardIds.MAIN_MENU_CARD,
                        projectionActive = true,
                        projectionStateMayBeStale = false,
                        hasManagedSecondaryDisplayWork = false,
                        cardCanAffectManagedAppBounds = false
                )

        assertTrue(decision.pushProjectionStateBeforeCard)
        assertTrue(decision.forceProjectionStateBeforeCard)
        assertTrue(decision.updateVirtualClusterVisibility)
        assertFalse(decision.syncSecondaryDisplayApps)
        assertTrue(decision.syncVisibleCardValues)
    }

    @Test
    fun staleProjectionStateIsClearedWithoutForcingWhenProjectionInactive() {
        val decision =
                ClusterCardFlowPolicy.decideCardChange(
                        nextCard = ClusterCardIds.NATIVE_CARD,
                        projectionActive = false,
                        projectionStateMayBeStale = true,
                        hasManagedSecondaryDisplayWork = false,
                        cardCanAffectManagedAppBounds = false
                )

        assertTrue(decision.pushProjectionStateBeforeCard)
        assertFalse(decision.forceProjectionStateBeforeCard)
        assertTrue(decision.updateVirtualClusterVisibility)
        assertFalse(decision.syncVisibleCardValues)
    }

    @Test
    fun managedDisplaySyncOnlyRunsWhenCardCanAffectBounds() {
        val neutralDecision =
                ClusterCardFlowPolicy.decideCardChange(
                        nextCard = ClusterCardIds.AIRCON_CARD,
                        projectionActive = false,
                        projectionStateMayBeStale = false,
                        hasManagedSecondaryDisplayWork = true,
                        cardCanAffectManagedAppBounds = false
                )

        assertFalse(neutralDecision.updateVirtualClusterVisibility)
        assertFalse(neutralDecision.clearAppliedAppConfigCache)
        assertFalse(neutralDecision.syncSecondaryDisplayApps)

        val boundsDecision =
                ClusterCardFlowPolicy.decideCardChange(
                        nextCard = ClusterCardIds.NATIVE_CARD,
                        projectionActive = false,
                        projectionStateMayBeStale = false,
                        hasManagedSecondaryDisplayWork = true,
                        cardCanAffectManagedAppBounds = true
                )

        assertTrue(boundsDecision.updateVirtualClusterVisibility)
        assertTrue(boundsDecision.clearAppliedAppConfigCache)
        assertTrue(boundsDecision.syncSecondaryDisplayApps)
    }

    @Test
    fun nativeCardPassThroughIsEnabledWhenNotProjecting() {
        // Master parity: no card nativo (0) e sem projecao, revelamos o cluster de fabrica.
        assertTrue(
                ClusterCardFlowPolicy.shouldUseNativeCardPassThrough(
                        cardId = ClusterCardIds.NATIVE_CARD,
                        warningActive = false,
                        projectionActive = false
                )
        )
    }

    @Test
    fun nativeCardPassThroughStillEnabledDuringWarnings() {
        // O cluster nativo mostra os proprios avisos, entao nao bloqueamos o pass-through
        // durante avisos (era isso que impedia de chegar no cluster original do carro).
        assertTrue(
                ClusterCardFlowPolicy.shouldUseNativeCardPassThrough(
                        cardId = ClusterCardIds.NATIVE_CARD,
                        warningActive = true,
                        projectionActive = false
                )
        )
    }

    @Test
    fun nativeCardPassThroughIsDisabledDuringProjection() {
        // Durante projecao AA/CarPlay/espelhamento nao interrompemos a sessao.
        assertFalse(
                ClusterCardFlowPolicy.shouldUseNativeCardPassThrough(
                        cardId = ClusterCardIds.NATIVE_CARD,
                        warningActive = false,
                        projectionActive = true
                )
        )
    }

    @Test
    fun nativeCardPassThroughIsDisabledForNonNativeCards() {
        assertFalse(
                ClusterCardFlowPolicy.shouldUseNativeCardPassThrough(
                        cardId = ClusterCardIds.MAIN_MENU_CARD,
                        warningActive = false,
                        projectionActive = false
                )
        )
    }
}
