package io.github.caiomc.nexo.core.conversation.usecase;

/**
 * Assinatura de uma conversa; fechar para de receber eventos.
 */
public interface ConversationWatch extends AutoCloseable {

    @Override
    void close();
}
