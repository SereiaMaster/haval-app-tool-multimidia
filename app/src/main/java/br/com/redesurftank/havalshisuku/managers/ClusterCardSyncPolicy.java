package br.com.redesurftank.havalshisuku.managers;

public final class ClusterCardSyncPolicy {
    private static final int MAIN_MENU_CARD = 1;
    private static final int AIRCON_CARD = 3;
    private static final int CLUSTER_KEY_LEFT = 1026;
    private static final int CLUSTER_KEY_RIGHT = 1027;
    private static final long NATIVE_CLUSTER_CARD_INPUT_WINDOW_MS = 2500L;

    private ClusterCardSyncPolicy() {
    }

    public static boolean shouldIgnoreNativeClusterCardChanged(
            int previousCard,
            int nextCard,
            long sinceInputMs,
            int lastInputKeyCode,
            long sinceSyntheticMs,
            int lastSyntheticTarget
    ) {
        if (previousCard == nextCard) return true;

        // A escolha sintetica do usuario (toque no volante) e autoritativa ate a proxima
        // navegacao sintetica: qualquer card divergente reportado pelo carro e ignorado
        // indefinidamente. Isso impede o cluster de "voltar pro menu" sozinho de tempos
        // em tempos quando o carro reenvia seu card padrao.
        if (lastSyntheticTarget >= 0) {
            return nextCard != lastSyntheticTarget;
        }

        if (nextCard != 0) return false;
        if (previousCard != MAIN_MENU_CARD && previousCard != AIRCON_CARD) return false;
        return !isRecentClusterCardNavigationInput(lastInputKeyCode, sinceInputMs);
    }

    private static boolean isRecentClusterCardNavigationInput(int lastInputKeyCode, long sinceInputMs) {
        return (lastInputKeyCode == CLUSTER_KEY_LEFT || lastInputKeyCode == CLUSTER_KEY_RIGHT)
                && sinceInputMs >= 0L
                && sinceInputMs <= NATIVE_CLUSTER_CARD_INPUT_WINDOW_MS;
    }
}
