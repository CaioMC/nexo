package io.github.caiomc.nexo.adapters.conversation.openai.mapper;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.caiomc.nexo.core.conversation.domain.enums.ToolExposure;
import io.github.caiomc.nexo.core.conversation.port.model.command.HistoryMessageCommand;
import io.github.caiomc.nexo.core.conversation.port.model.command.OpenModelSessionCommand;
import io.github.caiomc.nexo.core.conversation.port.model.command.ToolOutputCommand;
import io.github.caiomc.nexo.core.support.CatalogFixtures;
import io.github.caiomc.nexo.core.support.RetrievalTestDoubles.FixedCatalog;

import static org.assertj.core.api.Assertions.assertThat;

class ResponsesRequestMapperTest {

    private final ObjectMapper json = new ObjectMapper();
    private final ResponsesRequestMapper requests = new ResponsesRequestMapper(
            this.json,
            new ResponsesToolMapper(this.json, new FixedCatalog(CatalogFixtures.catalog()))
    );

    private final OpenModelSessionCommand command = new OpenModelSessionCommand(
            "seja breve",
            List.of(new HistoryMessageCommand(HistoryMessageCommand.USER, "oi"), new HistoryMessageCommand(HistoryMessageCommand.ASSISTANT, "olá")),
            "minha agenda",
            ToolExposure.CLIENT_TOOL_SEARCH,
            List.of(),
            List.of("google_calendar")
    );

    @Test
    void baseRequestCarriesModelInstructionsAndTheClientToolSearch() {
        ObjectNode request = this.requests.toBaseRequest("gpt-x", this.command);

        assertThat(request.path("model").asText()).isEqualTo("gpt-x");
        assertThat(request.path("instructions").asText()).isEqualTo("seja breve");
        assertThat(request.path("stream").asBoolean()).isTrue();
        assertThat(request.path("tools")).singleElement().satisfies(tool -> {
            assertThat(tool.path("type").asText()).isEqualTo("tool_search");
            assertThat(tool.path("description").asText()).contains("google_calendar");
        });
    }

    @Test
    void initialInputIsTheHistoryFollowedByTheUserMessage() {
        ArrayNode input = this.requests.toInitialInput(this.command);

        assertThat(input).hasSize(3);
        assertThat(input.get(2).path("role").asText()).isEqualTo(HistoryMessageCommand.USER);
        assertThat(input.get(2).path("content").asText()).isEqualTo("minha agenda");
    }

    @Test
    void toolOutputsKeepTheCallIdAndSearchResultsAreDeferred() {
        ObjectNode search = this.requests.toInputItem(new ToolOutputCommand.SearchResult("ts1", CatalogFixtures.tools().subList(0, 1)));
        ObjectNode function = this.requests.toInputItem(new ToolOutputCommand.FunctionResult("c1", "{\"ok\":true}"));

        assertThat(search.path("type").asText()).isEqualTo("tool_search_output");
        assertThat(search.path("call_id").asText()).isEqualTo("ts1");
        assertThat(search.path("tools").get(0).path("defer_loading").asBoolean()).isTrue();
        assertThat(function.path("type").asText()).isEqualTo("function_call_output");
        assertThat(function.path("output").asText()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void replayableItemDropsNullsAtAnyDepth() throws Exception {
        JsonNode item = this.requests.toReplayableItem(this.json.readTree("""
                {"type": "message", "status": null, "content": [{"text": "oi", "annotations": null}]}
                """));

        assertThat(item.has("status")).isFalse();
        assertThat(item.path("content").get(0).has("annotations")).isFalse();
        assertThat(item.path("content").get(0).path("text").asText()).isEqualTo("oi");
    }

    @Test
    void requestCopiesTheInputSoLaterItemsDoNotLeakIntoIt() {
        ObjectNode base = this.requests.toBaseRequest("gpt-x", this.command);
        ArrayNode input = this.requests.toInitialInput(this.command);
        ObjectNode request = this.requests.toRequest(base, input);

        input.addObject().put("role", "user").put("content", "depois");

        assertThat(request.path("input")).hasSize(3);
        assertThat(base.has("input")).isFalse();
    }
}
