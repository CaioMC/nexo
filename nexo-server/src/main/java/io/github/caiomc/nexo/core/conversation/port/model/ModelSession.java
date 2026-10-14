package io.github.caiomc.nexo.core.conversation.port.model;

import java.util.function.Consumer;

import io.github.caiomc.nexo.core.conversation.port.model.command.ModelCallCommand;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelDeltaData;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelOutputData;

/**
 * A conversa com o modelo durante um turno. O histórico do turno fica aqui dentro (sem
 * {@code previous_response_id}): cada {@link #call} reenvia tudo.
 */
public interface ModelSession extends AutoCloseable {

    /**
     * Uma chamada ao modelo. Leva as respostas aos pedidos da chamada anterior e devolve o que o
     * modelo produziu agora. Os pedaços de texto chegam em {@code onDelta}, na mesma thread.
     */
    ModelOutputData call(
            ModelCallCommand command,
            Consumer<ModelDeltaData> onDelta
    );

    /**
     * Interrompe a chamada em andamento (botão Parar). Pode ser chamado de outra thread.
     */
    void cancel();

    @Override
    void close();
}
