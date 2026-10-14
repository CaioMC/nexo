package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.catalog.domain.exception.DependencyCycleException;
import io.github.caiomc.nexo.core.catalog.domain.exception.ToolNotFoundException;
import io.github.caiomc.nexo.core.support.CatalogFixtures;

import static io.github.caiomc.nexo.core.support.CatalogFixtures.name;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.names;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class DependencyGraphTest {

    private final DependencyGraph graph = CatalogFixtures.graph();

    @Test
    void rejectsCycleShowingThePath() {
        assertThatThrownBy(() -> new DependencyGraph(
                names("a_tool", "b_tool", "c_tool"),
                Map.of(
                        name("a_tool"), names("b_tool"),
                        name("b_tool"), names("c_tool"),
                        name("c_tool"), names("a_tool")
                )
        ))
                .isInstanceOf(DependencyCycleException.class)
                .hasMessageContaining("a_tool -> b_tool -> c_tool -> a_tool");
    }

    @Test
    void rejectsSelfDependency() {
        assertThatThrownBy(() -> new DependencyGraph(names("a_tool"), Map.of(name("a_tool"), names("a_tool"))))
                .isInstanceOf(DependencyCycleException.class);
    }

    @Test
    void rejectsEdgeToUnknownTool() {
        assertThatThrownBy(() -> new DependencyGraph(names("a_tool"), Map.of(name("a_tool"), names("ghost_tool"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost_tool");
    }

    @Test
    void closureBringsDirectAndIndirectPrerequisites() {
        assertThat(this.graph.closureDAG(names("calendar_delete_event")))
                .containsExactly(
                        name("calendar_delete_event"),
                        name("calendar_get_event"),
                        name("calendar_list_calendars"),
                        name("calendar_list_events")
                );
    }

    @Test
    void levelIsTheLongestChainBelow() {
        assertThat(this.graph.levelOfDAG(name("calendar_list_calendars"))).isZero();
        assertThat(this.graph.levelOfDAG(name("calendar_create_event"))).isEqualTo(2);
        assertThat(this.graph.levelOfDAG(name("calendar_delete_event"))).isEqualTo(3);
    }

    @Test
    void executionOrderIsKahnByWavesAlphabeticalInsideTheWave() {
        ExecutionPlan plan = this.graph.executionOrderDAG(names("calendar_delete_event", "docs_summarize_shared"));

        assertThat(plan.steps())
                .extracting(PlanStep::tool, PlanStep::level)
                .containsExactly(
                        tuple(name("calendar_list_calendars"), 0),
                        tuple(name("docs_read"), 0),
                        tuple(name("drive_get_metadata"), 0),
                        tuple(name("calendar_list_events"), 1),
                        tuple(name("docs_summarize_shared"), 1),
                        tuple(name("calendar_get_event"), 2),
                        tuple(name("calendar_delete_event"), 3)
                );
        assertThat(plan.requested()).containsExactly(name("calendar_delete_event"), name("docs_summarize_shared"));
    }

    @Test
    void executionOrderRespectsEveryEdge() {
        List<ToolName> order = this.graph.executionOrderDAG(this.graph.tools()).tools();

        CatalogFixtures.edges().forEach((tool, prerequisites) -> prerequisites.forEach(prerequisite ->
                assertThat(order.indexOf(prerequisite)).isLessThan(order.indexOf(tool))
        ));
    }

    @Test
    void executionOrderIgnoresDependentsOutsideTheClosure() {
        assertThat(this.graph.executionOrderDAG(names("calendar_list_events")).tools())
                .containsExactly(name("calendar_list_calendars"), name("calendar_list_events"));
    }

    @Test
    void missingPrerequisitesAreTheDirectOnesNotExecuted() {
        assertThat(this.graph.missingPrerequisitesDAG(name("docs_summarize_shared"), Set.of(name("docs_read"))))
                .containsExactly(name("drive_get_metadata"));
        assertThat(this.graph.missingPrerequisitesDAG(name("docs_summarize_shared"), Set.copyOf(names("docs_read", "drive_get_metadata"))))
                .isEmpty();
    }

    @Test
    void dependentsAreTheReverseEdges() {
        assertThat(this.graph.dependentsOfDAG(name("calendar_list_events")))
                .containsExactly(name("calendar_create_event"), name("calendar_get_event"));
        assertThat(this.graph.edgeCount()).isEqualTo(7);
    }

    @Test
    void unknownToolIsNotFound() {
        assertThatThrownBy(() -> this.graph.closureDAG(names("ghost_tool")))
                .isInstanceOf(ToolNotFoundException.class);
    }
}
