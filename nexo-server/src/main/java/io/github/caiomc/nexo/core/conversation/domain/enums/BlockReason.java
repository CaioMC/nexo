package io.github.caiomc.nexo.core.conversation.domain.enums;

/**
 * Por que o {@code ToolGatekeeperService} barrou uma chamada.
 */
public enum BlockReason {

    /**
     * RN-04: pré-requisito direto ainda não rodou com sucesso na conversa.
     */
    MISSING_PREREQUISITES,

    /**
     * RN-03: o contexto da ferramenta está desligado agora.
     */
    CONTEXT_DISABLED
}
