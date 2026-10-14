package io.github.caiomc.nexo.adapters.catalog.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.caiomc.nexo.adapters.catalog.web.handler.CatalogExceptionHandler;
import io.github.caiomc.nexo.adapters.catalog.web.mapper.CatalogWebMapper;
import io.github.caiomc.nexo.core.catalog.domain.exception.ToolNotFoundException;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.catalog.usecase.DescribeCatalogUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.GetToolUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.ListToolsUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.PlanExecutionUseCase;
import io.github.caiomc.nexo.core.catalog.usecase.command.GetToolCommand;
import io.github.caiomc.nexo.core.catalog.usecase.command.PlanExecutionCommand;
import io.github.caiomc.nexo.core.support.CatalogFixtures;
import io.github.caiomc.nexo.support.TestUsers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@Import({CatalogWebMapper.class, CatalogExceptionHandler.class})
class CatalogControllerTest {

    private final ToolCatalog catalog = CatalogFixtures.catalog();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DescribeCatalogUseCase describe;

    @MockitoBean
    private ListToolsUseCase list;

    @MockitoBean
    private GetToolUseCase get;

    @MockitoBean
    private PlanExecutionUseCase plan;

    @Test
    void overview() throws Exception {
        when(this.describe.describe()).thenReturn(this.catalog.overview());

        this.mvc.perform(get("/api/catalog").with(TestUsers.ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tools").value(10))
                .andExpect(jsonPath("$.dependencies").value(7))
                .andExpect(jsonPath("$.byContext[0].context").value("calendar"));
    }

    @Test
    void toolDetailWithNeighbours() throws Exception {
        when(this.get.get(new GetToolCommand(ToolName.of("calendar_list_events")))).thenReturn(this.catalog.detailDAG(ToolName.of("calendar_list_events")));

        this.mvc.perform(get("/api/catalog/tools/calendar_list_events").with(TestUsers.ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dependsOn[0]").value("calendar_list_calendars"))
                .andExpect(jsonPath("$.dependents.length()").value(2))
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    void unknownToolIs404() throws Exception {
        when(this.get.get(any())).thenThrow(new ToolNotFoundException(ToolName.of("ghost_tool")));

        this.mvc.perform(get("/api/catalog/tools/ghost_tool").with(TestUsers.ana()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://nexo.dev/problems/ferramenta-nao-encontrada"));
    }

    @Test
    void invalidToolNameIs400() throws Exception {
        this.mvc.perform(get("/api/catalog/tools/Nome-Invalido").with(TestUsers.ana()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://nexo.dev/problems/requisicao-invalida"));
    }

    @Test
    void planReturnsKahnOrder() throws Exception {
        when(this.plan.planDAG(new PlanExecutionCommand(CatalogFixtures.names("calendar_create_event"))))
                .thenReturn(this.catalog.graph().executionOrderDAG(CatalogFixtures.names("calendar_create_event")));

        this.mvc.perform(get("/api/catalog/plan").param("tools", "calendar_create_event").with(TestUsers.ana()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].tool").value("calendar_list_calendars"))
                .andExpect(jsonPath("$.steps[2].tool").value("calendar_create_event"))
                .andExpect(jsonPath("$.steps[2].level").value(2));
    }

    @Test
    void planWithoutToolsIs400() throws Exception {
        this.mvc.perform(get("/api/catalog/plan").with(TestUsers.ana()))
                .andExpect(status().isBadRequest());
    }
}
