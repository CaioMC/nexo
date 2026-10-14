package io.github.caiomc.nexo.core.conversation.usecase;

public interface ExpireApprovalsUseCase {

    /**
     * Vence as aprovações pendentes fora do prazo (RN-06).
     *
     * @return quantas venceram
     */
    int expire();
}
