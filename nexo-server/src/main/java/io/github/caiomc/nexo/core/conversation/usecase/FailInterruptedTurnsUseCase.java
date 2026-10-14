package io.github.caiomc.nexo.core.conversation.usecase;

public interface FailInterruptedTurnsUseCase {

    /**
     * Na subida: turnos que estavam ativos quando o servidor parou viram {@code SERVER_RESTARTED} (RNF-08).
     *
     * @return quantos turnos foram encerrados
     */
    int recover();
}
