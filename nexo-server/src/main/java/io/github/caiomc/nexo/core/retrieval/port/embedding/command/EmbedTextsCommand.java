package io.github.caiomc.nexo.core.retrieval.port.embedding.command;

import java.util.List;

/**
 * Pedido de vetores para uma lista de textos. A resposta vem na mesma ordem.
 */
public record EmbedTextsCommand(List<String> texts) {

    public EmbedTextsCommand {
        texts = List.copyOf(texts);
    }
}
