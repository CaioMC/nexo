package io.github.caiomc.nexo.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;
import io.github.caiomc.nexo.support.TestUsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ponta a ponta com o banco real: catálogo, plano (Kahn em Java) e busca RAG + DAG filtrada pelos
 * contextos do usuário logado. Sem chave, a busca cai para FULL_TEXT e nada vai para a rede.
 */
@AutoConfigureMockMvc
class SearchApiIT extends PostgresIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private SessionUser user;

    @BeforeEach
    void user() {
        this.user = newUser(this.jdbc, "busca@exemplo.com");
        enable(this.jdbc, this.user, "calendar", "sheets", "gmail");
    }

    @Test
    void catalogOverview() throws Exception {
        this.mvc.perform(get("/api/catalog").with(TestUsers.as(this.user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts").value(8))
                .andExpect(jsonPath("$.namespaces").value(16))
                .andExpect(jsonPath("$.tools").value(50))
                .andExpect(jsonPath("$.dependencies").value(33));
    }

    @Test
    void planOfUpdateValuesFollowsTheDag() throws Exception {
        this.mvc.perform(get("/api/catalog/plan").param("tools", "google_sheets_update_values").with(TestUsers.as(this.user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].tool").value("google_sheets_get_spreadsheet"))
                .andExpect(jsonPath("$.steps[1].tool").value("google_sheets_get_values"))
                .andExpect(jsonPath("$.steps[2].tool").value("google_sheets_update_values"))
                .andExpect(jsonPath("$.steps[2].level").value(2));
    }

    @Test
    void searchBringsTheEventToolAndItsPrerequisitesAndSavesHistory() throws Exception {
        long before = this.jdbc.sql("SELECT count(*) FROM tool_search").query(Long.class).single();

        this.mvc.perform(post("/api/search")
                        .with(TestUsers.as(this.user))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"crie um evento na agenda amanhã às 10h\", \"topK\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("FULL_TEXT"))
                .andExpect(jsonPath("$.tools[0].name").value("google_calendar_list_calendars"))
                .andExpect(jsonPath("$.tools[0].origin").value("DAG"))
                .andExpect(jsonPath("$.tools[?(@.name == 'google_calendar_create_event')].origin").value("RAG"));

        assertThat(this.jdbc.sql("SELECT count(*) FROM tool_search").query(Long.class).single()).isEqualTo(before + 1);

        this.mvc.perform(get("/api/search/history").with(TestUsers.as(this.user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].query").value("crie um evento na agenda amanhã às 10h"))
                .andExpect(jsonPath("$[0].allowedContexts.length()").value(3));
    }

    @Test
    void disabledContextBecomesBlockedSuggestion() throws Exception {
        SessionUser docsOnly = newUser(this.jdbc, "docs@exemplo.com");

        enable(this.jdbc, docsOnly, "docs");

        this.mvc.perform(post("/api/search")
                        .with(TestUsers.as(docsOnly))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"crie um evento na agenda amanhã às 10h\", \"topK\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tools[?(@.name == 'google_calendar_create_event')]").isEmpty())
                .andExpect(jsonPath("$.blocked[0].missingContexts[0]").value("calendar"));
    }

    @Test
    void withoutSessionIs401() throws Exception {
        this.mvc.perform(get("/api/catalog"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://nexo.dev/problems/nao-autenticado"));
    }

    @Test
    void toolDetail() throws Exception {
        this.mvc.perform(get("/api/catalog/tools/google_gmail_modify_message").with(TestUsers.as(this.user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effect").value("WRITE"))
                .andExpect(jsonPath("$.dependsOn.length()").value(2))
                .andExpect(jsonPath("$.parameters[1].type").value("array"))
                .andExpect(jsonPath("$.parameters[1].items.type").value("string"));
    }
}
