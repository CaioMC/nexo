package io.github.caiomc.nexo.core.catalog.domain.exception;

import java.util.List;
import java.util.stream.Collectors;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;

/**
 * O grafo de dependências tem ciclo (RN-08). A mensagem mostra o caminho para saber qual aresta tirar.
 */
public class DependencyCycleException extends IllegalStateException {

    public DependencyCycleException(List<ToolName> path) {
        super("Ciclo de dependências entre ferramentas: " + path.stream().map(ToolName::value).collect(Collectors.joining(" -> ")) + ". Remova uma das arestas do ciclo em docs/catalogo/google-tools.json.");
    }
}
