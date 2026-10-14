package io.github.caiomc.nexo.core.conversation.port.tool.data;

/**
 * Resultado de uma chamada ao Google.
 *
 * @param output    o que vai ao modelo (JSON), já truncado em 16 mil caracteres (RNF-04)
 * @param summary   frase curta para o cartão da ferramenta e para o histórico dos próximos turnos
 * @param error     mensagem de erro; nulo se deu certo
 * @param truncated a saída passou do limite e foi cortada
 */
public record ToolResultData(
        ToolOutcome outcome,
        String output,
        String summary,
        String error,
        boolean truncated
) {

    public boolean succeeded() {
        return this.outcome == ToolOutcome.SUCCEEDED;
    }
}
