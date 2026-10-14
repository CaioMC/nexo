package io.github.caiomc.nexo.core.conversation.domain.model;

/**
 * Consumo acumulado do turno.
 */
public record Usage(
        int modelCalls,
        long inputTokens,
        long cachedInputTokens,
        long outputTokens
) {

    public static Usage none() {
        return new Usage(
                0,
                0,
                0,
                0
        );
    }

    /**
     * Mais uma chamada ao modelo.
     */
    public Usage plus(
            long input,
            long cached,
            long output
    ) {
        return new Usage(
                this.modelCalls + 1,
                this.inputTokens + input,
                this.cachedInputTokens + cached,
                this.outputTokens + output
        );
    }
}
