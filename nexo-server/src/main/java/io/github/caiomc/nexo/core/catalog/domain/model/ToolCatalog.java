package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.caiomc.nexo.core.catalog.domain.exception.ToolNotFoundException;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.isNull;

/**
 * O catálogo inteiro em memória: as ferramentas e o grafo de dependências entre elas. Lido do
 * banco uma vez na subida ({@code CatalogLoaderService}) e imutável depois disso.
 */
public final class ToolCatalog {

    private final Map<ToolName, Tool> tools;
    private final DependencyGraph graph;

    public ToolCatalog(
            Collection<Tool> tools,
            DependencyGraph dependencyGraph
    ) {
        Map<ToolName, Tool> indexed = tools.stream()
                .collect(Collectors.toMap(
                        Tool::name,
                        Function.identity(),
                        this::duplicated,
                        TreeMap::new
                ));

        if (!indexed.keySet().equals(dependencyGraph.tools())) {
            throw new IllegalArgumentException("O grafo de dependências e o catálogo não têm as mesmas ferramentas");
        }

        this.tools = Collections.unmodifiableMap(indexed);
        this.graph = dependencyGraph;
    }

    public DependencyGraph graph() {
        return this.graph;
    }

    public Collection<Tool> tools() {
        return this.tools.values();
    }

    public int size() {
        return this.tools.size();
    }

    public Optional<Tool> find(ToolName name) {
        return Optional.ofNullable(this.tools.get(name));
    }

    public Tool require(ToolName name) {
        return this.find(name).orElseThrow(() -> new ToolNotFoundException(name));
    }

    public List<Tool> filter(
            ContextKey context,
            String namespace
    ) {
        return this.tools.values()
                .stream()
                .filter(tool -> isNull(context) || tool.context().equals(context))
                .filter(tool -> isNull(namespace) || tool.namespace().equals(namespace))
                .toList();
    }

    public ToolDetail detailDAG(ToolName name) {
        return new ToolDetail(
                this.require(name),
                List.copyOf(this.graph.prerequisitesOfDAG(name)),
                List.copyOf(this.graph.dependentsOfDAG(name)),
                this.graph.levelOfDAG(name)
        );
    }

    /**
     * RN-02: a ferramenta só está disponível se o contexto dela e os de todo o seu fecho de
     * pré-requisitos estão ligados. {@code enabled} nulo = sem filtro (todas disponíveis).
     * As duas listas mantêm a ordem de {@code requested}.
     */
    public Availability availabilityDAG(
            List<ToolName> requested,
            Set<ContextKey> enabled
    ) {
        List<ToolName> available = new ArrayList<>();
        List<BlockedTool> blocked = new ArrayList<>();

        for (ToolName name : requested) {
            SortedSet<ContextKey> missing = isNull(enabled) ? new TreeSet<>() : this.missingContextsDAG(name, enabled);

            if (missing.isEmpty()) {
                available.add(name);
            } else {
                blocked.add(new BlockedTool(name, List.copyOf(missing)));
            }
        }

        return new Availability(available, blocked);
    }

    public CatalogOverview overview() {
        Map<ContextKey, List<Tool>> byContext = this.tools.values()
                .stream()
                .collect(Collectors.groupingBy(Tool::context, TreeMap::new, Collectors.toList()));
        List<ContextSummary> summaries = byContext.entrySet()
                .stream()
                .map(entry -> new ContextSummary(entry.getKey(), entry.getValue().size(), this.namespaces(entry.getValue())))
                .toList();

        return new CatalogOverview(
                byContext.size(),
                summaries.stream().mapToInt(summary -> summary.namespaces().size()).sum(),
                this.tools.size(),
                this.graph.edgeCount(),
                summaries
        );
    }

    private SortedSet<ContextKey> missingContextsDAG(
            ToolName name,
            Set<ContextKey> enabled
    ) {
        return this.graph.closureDAG(List.of(name))
                .stream()
                .map(tool -> this.require(tool).context())
                .filter(context -> !enabled.contains(context))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<String> namespaces(List<Tool> tools) {
        return tools.stream().map(Tool::namespace).distinct().sorted().toList();
    }

    private Tool duplicated(
            Tool first,
            Tool second
    ) {
        throw new IllegalArgumentException("Ferramenta repetida no catálogo: " + first.name());
    }
}
