package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import io.github.caiomc.nexo.core.catalog.domain.exception.DependencyCycleException;
import io.github.caiomc.nexo.core.catalog.domain.exception.ToolNotFoundException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * O DAG das ferramentas, resolvido em Java (o banco só guarda as arestas).
 *
 * <p>Responde às três perguntas que o RAG sozinho não responde:
 * <ol>
 *   <li>{@link #closureDAG}: se vou usar A, de quais outras preciso (fecho transitivo)?</li>
 *   <li>{@link #executionOrderDAG}: em que ordem executá-las (algoritmo de Kahn por levas)?</li>
 *   <li>{@link #missingPrerequisitesDAG}: posso executar A agora (trava em tempo de execução)?</li>
 * </ol>
 *
 * <p>Imutável. O construtor recusa ciclo (RN-08) e aresta para ferramenta desconhecida, então todo
 * percurso termina. Empates são desfeitos pela ordem alfabética: mesma entrada, mesma saída.
 */
public final class DependencyGraph {

    private static final int WHITE = 0;
    private static final int GRAY = 1;
    private static final int BLACK = 2;

    private final Map<ToolName, SortedSet<ToolName>> prerequisites;
    private final Map<ToolName, SortedSet<ToolName>> dependents;
    private final Map<ToolName, Integer> levels;
    private final int edges;

    /**
     * @param tools        todas as ferramentas do catálogo (os nós)
     * @param requirements para cada ferramenta, as ferramentas de que ela depende (as arestas)
     */
    public DependencyGraph(
            Collection<ToolName> tools,
            Map<ToolName, ? extends Collection<ToolName>> requirements
    ) {
        Map<ToolName, SortedSet<ToolName>> outgoing = new TreeMap<>();
        Map<ToolName, SortedSet<ToolName>> incoming = new TreeMap<>();

        for (ToolName tool : tools) {
            outgoing.put(tool, new TreeSet<>());
            incoming.put(tool, new TreeSet<>());
        }

        int count = 0;

        for (Map.Entry<ToolName, ? extends Collection<ToolName>> entry : requirements.entrySet()) {
            ToolName tool = entry.getKey();

            for (ToolName prerequisite : entry.getValue()) {
                this.requireKnown(outgoing, tool, prerequisite);
                this.requireKnown(outgoing, prerequisite, tool);

                if (tool.equals(prerequisite)) {
                    throw new DependencyCycleException(List.of(tool, tool));
                }

                if (outgoing.get(tool).add(prerequisite)) {
                    incoming.get(prerequisite).add(tool);
                    count++;
                }
            }
        }

        this.prerequisites = this.freeze(outgoing);
        this.dependents = this.freeze(incoming);
        this.edges = count;

        this.rejectCyclesDAG();
        this.levels = this.computeLevelsDAG();
    }

    public Set<ToolName> tools() {
        return this.prerequisites.keySet();
    }

    public int edgeCount() {
        return this.edges;
    }

    public SortedSet<ToolName> prerequisitesOfDAG(ToolName tool) {
        return this.require(tool, this.prerequisites);
    }

    public SortedSet<ToolName> dependentsOfDAG(ToolName tool) {
        return this.require(tool, this.dependents);
    }

    /**
     * Nível no DAG: a maior cadeia de pré-requisitos abaixo da ferramenta (0 = não depende de nada).
     * É o número da leva em que ela fica pronta no Kahn.
     */
    public int levelOfDAG(ToolName tool) {
        this.require(tool, this.prerequisites);

        return this.levels.get(tool);
    }

    /**
     * As ferramentas pedidas mais todos os pré-requisitos, diretos e indiretos (busca em largura).
     */
    public SortedSet<ToolName> closureDAG(Collection<ToolName> tools) {
        SortedSet<ToolName> visited = new TreeSet<>();
        Deque<ToolName> pending = new ArrayDeque<>();

        for (ToolName tool : tools) {
            this.require(tool, this.prerequisites);
            pending.add(tool);
        }

        while (!pending.isEmpty()) {
            ToolName current = pending.poll();

            if (visited.add(current)) {
                pending.addAll(this.prerequisites.get(current));
            }
        }

        return Collections.unmodifiableSortedSet(visited);
    }

    /**
     * Ordenação topológica do fecho pelo algoritmo de Kahn, processado por levas: primeiro as que
     * não têm pré-requisito no fecho, depois as que só dependiam delas, e assim por diante. Dentro
     * da leva, ordem alfabética.
     */
    public ExecutionPlan executionOrderDAG(Collection<ToolName> requested) {
        SortedSet<ToolName> closure = this.closureDAG(requested);
        Map<ToolName, Integer> inDegree = new HashMap<>();

        for (ToolName tool : closure) {
            inDegree.put(tool, this.prerequisites.get(tool).size());
        }

        List<PlanStep> steps = new ArrayList<>();
        SortedSet<ToolName> wave = this.readyDAG(inDegree);
        int level = 0;

        while (!wave.isEmpty()) {
            SortedSet<ToolName> next = new TreeSet<>();

            for (ToolName tool : wave) {
                steps.add(new PlanStep(
                        steps.size() + 1,
                        tool,
                        level,
                        List.copyOf(this.prerequisites.get(tool))
                ));
                inDegree.remove(tool);

                for (ToolName dependent : this.dependents.get(tool)) {
                    if (!inDegree.containsKey(dependent)) {
                        continue;
                    }

                    int remaining = inDegree.merge(dependent, -1, Integer::sum);

                    if (remaining == 0) {
                        next.add(dependent);
                    }
                }
            }

            wave = next;
            level++;
        }

        return new ExecutionPlan(List.copyOf(new LinkedHashSet<>(requested)), steps);
    }

    /**
     * Pré-requisitos diretos que ainda não rodaram. Vazio = pode executar.
     */
    public List<ToolName> missingPrerequisitesDAG(
            ToolName tool,
            Set<ToolName> executed
    ) {
        return this.prerequisitesOfDAG(tool)
                .stream()
                .filter(prerequisite -> !executed.contains(prerequisite))
                .toList();
    }

    private SortedSet<ToolName> readyDAG(Map<ToolName, Integer> inDegree) {
        SortedSet<ToolName> ready = new TreeSet<>();

        inDegree.forEach((tool, degree) -> {
            if (degree == 0) {
                ready.add(tool);
            }
        });

        return ready;
    }

    private void rejectCyclesDAG() {
        Map<ToolName, Integer> color = new HashMap<>();

        for (ToolName tool : this.prerequisites.keySet()) {
            if (color.getOrDefault(tool, WHITE) == WHITE) {
                this.visitDAG(tool, color, new ArrayList<>());
            }
        }
    }

    private void visitDAG(
            ToolName tool,
            Map<ToolName, Integer> color,
            List<ToolName> path
    ) {
        color.put(tool, GRAY);
        path.add(tool);

        for (ToolName prerequisite : this.prerequisites.get(tool)) {
            int state = color.getOrDefault(prerequisite, WHITE);

            if (state == GRAY) {
                List<ToolName> cycle = new ArrayList<>(path.subList(path.indexOf(prerequisite), path.size()));

                cycle.add(prerequisite);
                throw new DependencyCycleException(cycle);
            }

            if (state == WHITE) {
                this.visitDAG(prerequisite, color, path);
            }
        }

        path.remove(path.size() - 1);
        color.put(tool, BLACK);
    }

    private Map<ToolName, Integer> computeLevelsDAG() {
        Map<ToolName, Integer> computed = new HashMap<>();

        for (ToolName tool : this.prerequisites.keySet()) {
            this.levelDAG(tool, computed);
        }

        return Map.copyOf(computed);
    }

    private int levelDAG(
            ToolName tool,
            Map<ToolName, Integer> computed
    ) {
        Integer known = computed.get(tool);

        if (nonNull(known)) {
            return known;
        }

        int level = 0;

        for (ToolName prerequisite : this.prerequisites.get(tool)) {
            level = Math.max(level, this.levelDAG(prerequisite, computed) + 1);
        }

        computed.put(tool, level);

        return level;
    }

    private void requireKnown(
            Map<ToolName, SortedSet<ToolName>> nodes,
            ToolName tool,
            ToolName other
    ) {
        if (!nodes.containsKey(tool)) {
            throw new IllegalArgumentException("Aresta " + tool + " -> " + other + " cita uma ferramenta que não está no catálogo");
        }
    }

    private SortedSet<ToolName> require(
            ToolName tool,
            Map<ToolName, SortedSet<ToolName>> index
    ) {
        SortedSet<ToolName> found = index.get(tool);

        if (isNull(found)) {
            throw new ToolNotFoundException(tool);
        }

        return found;
    }

    private Map<ToolName, SortedSet<ToolName>> freeze(Map<ToolName, SortedSet<ToolName>> source) {
        Map<ToolName, SortedSet<ToolName>> frozen = new TreeMap<>();

        source.forEach((tool, names) -> frozen.put(tool, Collections.unmodifiableSortedSet(names)));

        return Collections.unmodifiableMap(frozen);
    }
}
