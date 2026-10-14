package io.github.caiomc.nexo.core.catalog.domain.model;

/**
 * O que a ferramenta faz no Google. Leituras rodam direto; alterações exigem aprovação do usuário
 * ({@code warning}), e as que apagam ou somem com algo, aprovação {@code danger}.
 */
public enum ToolEffect {

    READ,
    WRITE,
    DESTRUCTIVE;

    public boolean requiresApproval() {
        return this != READ;
    }
}
