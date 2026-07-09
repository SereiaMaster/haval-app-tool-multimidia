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
    fun syntheticTargetStaysAuthoritativeAfterEchoWindow() {
        // Mesmo bem depois da antiga janela de eco, o alvo escolhido pelo usuario continua
        // autoritativo: o carro tentando voltar para o menu (1) deve ser ignorado.
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                3,      // previousCard (aircon, escolhido pelo usuario)
                1,      // nextCard (carro reafirmando o menu)
                5000L,
                1027,
                5000L,  // muito depois da antiga janela de 1500ms
                3       // lastSyntheticTarget (aircon)
            )
        )
    }

    @Test
    fun syntheticTargetAcceptsMatchingCardAfterEchoWindow() {
        // Se o carro informar exatamente o card alvo, aplicar e correto (nao ignora).
        assertFalse(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                3,
                5000L,
                1027,
                5000L,
                3
            )
        )
    }
}
