package io.github.caiomc.nexo.core.catalog.domain.model;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.caiomc.nexo.core.support.CatalogFixtures;

import static io.github.caiomc.nexo.core.support.CatalogFixtures.CALENDAR;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.DOCS;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.DRIVE;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.name;
import static io.github.caiomc.nexo.core.support.CatalogFixtures.names;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolCatalogTest {

    private final ToolCatalog catalog = CatalogFixtures.catalog();

    @Test
    void toolIsBlockedWhenItsOwnContextIsDisabled() {
        Availability availability = this.catalog.availabilityDAG(names("gmail_search", "docs_read"), Set.of(DOCS));

        assertThat(availability.available()).containsExactly(name("docs_read"));
        assertThat(availability.blocked()).containsExactly(new BlockedTool(name("gmail_search"), List.of(CatalogFixtures.GMAIL)));
    }

    @Test
    void crossContextAvailabilityLooksAtTheWholeClosure() {
        Availability withoutDrive = this.catalog.availabilityDAG(names("docs_summarize_shared", "docs_replace_text"), Set.of(DOCS));
        Availability withDrive = this.catalog.availabilityDAG(names("docs_summarize_shared"), Set.of(DOCS, DRIVE));

        assertThat(withoutDrive.available()).containsExactly(name("docs_replace_text"));
        assertThat(withoutDrive.blocked()).containsExactly(new BlockedTool(name("docs_summarize_shared"), List.of(DRIVE)));
        assertThat(withDrive.available()).containsExactly(name("docs_summarize_shared"));
    }

    @Test
    void nullEnabledContextsMeansNoFilter() {
        assertThat(this.catalog.availabilityDAG(names("gmail_search", "calendar_list_events"), null).available())
                .containsExactly(name("gmail_search"), name("calendar_list_events"));
    }

    @Test
    void detailShowsNeighboursAndLevel() {
        ToolDetail detail = this.catalog.detailDAG(name("calendar_list_events"));

        assertThat(detail.dependsOn()).containsExactly(name("calendar_list_calendars"));
        assertThat(detail.dependents()).containsExactly(name("calendar_create_event"), name("calendar_get_event"));
        assertThat(detail.level()).isEqualTo(1);
    }

    @Test
    void overviewCountsByContext() {
        CatalogOverview overview = this.catalog.overview();

        assertThat(overview.tools()).isEqualTo(10);
        assertThat(overview.contexts()).isEqualTo(4);
        assertThat(overview.dependencies()).isEqualTo(7);
        assertThat(overview.byContext()).first().satisfies(summary -> {
            assertThat(summary.context()).isEqualTo(CALENDAR);
            assertThat(summary.tools()).isEqualTo(5);
        });
    }

    @Test
    void filterByContext() {
        assertThat(this.catalog.filter(DRIVE, null)).extracting(Tool::name).containsExactly(name("drive_get_metadata"));
    }

    @Test
    void rejectsGraphWithDifferentTools() {
        assertThatThrownBy(() -> new ToolCatalog(CatalogFixtures.tools().subList(0, 3), CatalogFixtures.graph()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
