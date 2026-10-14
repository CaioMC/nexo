package io.github.caiomc.nexo.adapters.retrieval.web.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.caiomc.nexo.adapters.retrieval.web.handler.RetrievalExceptionHandler;
import io.github.caiomc.nexo.adapters.retrieval.web.mapper.SearchWebMapper;
import io.github.caiomc.nexo.core.catalog.domain.model.BlockedTool;
import io.github.caiomc.nexo.core.retrieval.domain.enums.RetrievalStrategy;
import io.github.caiomc.nexo.core.retrieval.domain.model.RankedTool;
import io.github.caiomc.nexo.core.retrieval.domain.model.ToolSelection;
import io.github.caiomc.nexo.core.retrieval.usecase.ListSearchHistoryUseCase;
import io.github.caiomc.nexo.core.retrieval.usecase.SelectToolsUseCase;
import io.github.caiomc.nexo.core.retrieval.usecase.command.SelectToolsCommand;
import io.github.caiomc.nexo.core.support.CatalogFixtures;
import io.github.caiomc.nexo.support.TestUsers;

import static io.github.caiomc.nexo.core.support.CatalogFixtures.name;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({SearchWebMapper.class, RetrievalExceptionHandler.class})
class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SelectToolsUseCase select;

    @MockitoBean
    private ListSearchHistoryUseCase history;

    @Test
    void returnsRagAndDagToolsInOrder() throws Exception {
        ToolSelection selection = ToolSelection.of(
                "marque reunião",
                RetrievalStrategy.HYBRID,
                1,
                List.of(new RankedTool(name("calendar_create_event"), 0.03)),
                CatalogFixtures.graph().executionOrderDAG(CatalogFixtures.names("calendar_create_event")),
                List.of(new BlockedTool(name("gmail_search"), List.of(CatalogFixtures.GMAIL))),
                4
        ).withSearchId(7);

        when(this.select.selectRAGDAG(new SelectToolsCommand("marque reunião", RetrievalStrategy.HYBRID, 1, null, TestUsers.anaId(), null))).thenReturn(selection);

        this.mvc.perform(post("/api/search")
                        .with(TestUsers.ana())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"marque reunião\", \"strategy\": \"hybrid\", \"topK\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchId").value(7))
                .andExpect(jsonPath("$.tools[0].name").value("calendar_list_calendars"))
                .andExpect(jsonPath("$.tools[0].origin").value("DAG"))
                .andExpect(jsonPath("$.tools[2].origin").value("RAG"))
                .andExpect(jsonPath("$.blocked[0].missingContexts[0]").value("gmail"));
    }

    @Test
    void blankQueryIs400() throws Exception {
        this.mvc.perform(post("/api/search").with(TestUsers.ana()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"query\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://nexo.dev/problems/requisicao-invalida"));
    }

    @Test
    void unknownStrategyIs400() throws Exception {
        when(this.select.selectRAGDAG(any())).thenThrow(new IllegalStateException("não deveria chegar aqui"));

        this.mvc.perform(post("/api/search").with(TestUsers.ana()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"query\": \"x\", \"strategy\": \"bm25\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("FULL_TEXT, VECTOR ou HYBRID")));
    }
}
