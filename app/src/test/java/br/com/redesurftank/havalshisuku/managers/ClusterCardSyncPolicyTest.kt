package br.com.redesurftank.havalshisuku.managers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterCardSyncPolicyTest {
    @Test
    fun recentSyntheticMainMenuNavigationIgnoresDivergentNativeAcEcho() {
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                3,
                616L,
                1027,
                616L,
                1
            )
        )
    }

    @Test
    fun recentSyntheticNavigationIgnoresMatchingNativeEcho() {
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                1,
                616L,
                1027,
                616L,
                1
            )
        )
    }

    @Test
    fun staleNativeZeroWithoutRecentInputIsIgnored() {
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                0,
                -1L,
                -1,
                -1L,
                -1
            )
        )
    }

    @Test
    fun nativeCardChangeWithoutSyntheticNavigationIsAccepted() {
        assertFalse(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                3,
                -1L,
                -1,
                -1L,
                -1
            )
        )
    }

    @Test
    fun syntheticTargetStaysAuthoritativeAfterLongDelay() {
        // Mesmo muito tempo depois da navegacao sintetica, o card escolhido pelo usuario
        // continua autoritativo: um eco divergente do carro (menu) e ignorado, evitando o
        // "voltar pro menu" sozinho.
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                3,
                1,
                120000L,
                -1,
                120000L,
                3
            )
        )
    }

    @Test
    fun syntheticTargetAcceptsMatchingCardAfterLongDelay() {
        // Um eco do carro que coincide com o alvo sintetico nao precisa mudar nada
        // (previousCard == nextCard), entao e ignorado.
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                3,
                3,
                120000L,
                -1,
                120000L,
                3
            )
        )
    }
}
