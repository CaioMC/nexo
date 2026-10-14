package io.github.caiomc.nexo.core.conversation.port.model;

import io.github.caiomc.nexo.core.conversation.port.model.command.OpenModelSessionCommand;

/**
 * O modelo de linguagem. Trocar de provedor = outro adapter desta porta.
 *
 * <p>Uso num turno: {@link #openSession} uma vez, depois {@link ModelSession#call} quantas vezes o
 * laço do agente precisar, e por fim {@link ModelSession#close}.
 */
public interface LanguageModelPort {

    /**
     * Prepara a conversa com o modelo para um turno. Não chama o modelo ainda.
     */
    ModelSession openSession(OpenModelSessionCommand command);

    /**
     * @return {@code false} se falta a chave da API: o turno nem começa
     */
    boolean isConfigured();

    /**
     * O nome do modelo usado, gravado no turno.
     */
    String modelName();
}
