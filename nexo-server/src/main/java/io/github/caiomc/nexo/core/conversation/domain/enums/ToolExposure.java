package io.github.caiomc.nexo.core.conversation.domain.enums;

/**
 * Como o modelo recebe as ferramentas (doc 09, seção 5).
 */
public enum ToolExposure {

    /**
     * Padrão: o modelo pede com {@code tool_search} e o servidor responde com RAG + DAG.
     */
    CLIENT_TOOL_SEARCH,

    /**
     * O servidor busca com a mensagem do usuário antes da 1ª chamada e manda as funções direto.
     */
    PRESELECTED
}
