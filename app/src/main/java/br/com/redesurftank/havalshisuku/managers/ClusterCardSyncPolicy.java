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

        // Uma vez que o usuario navegou pelos botoes do volante, a escolha dele e
        // autoritativa e permanece ate a proxima navegacao: ignoramos qualquer mudanca
        // de card vinda do carro (msgId 133) que contrarie o alvo sintetico, sem depender
        // do tempo. Isso evita o cluster "voltar para o menu" sozinho quando o carro
        // reafirma seu card padrao depois de alguns segundos.
        if (lastSyntheticTarget >= 0) {
            return nextCard != lastSyntheticTarget;
        }

        // Estado inicial (nenhuma navegacao sintetica ainda): mantem o comportamento
        // original, honrando o card informado pelo carro.
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
