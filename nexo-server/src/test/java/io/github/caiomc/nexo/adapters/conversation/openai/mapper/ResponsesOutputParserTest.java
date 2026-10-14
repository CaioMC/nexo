package io.github.caiomc.nexo.adapters.conversation.openai.mapper;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelOutputData;

import static org.assertj.core.api.Assertions.assertThat;

class ResponsesOutputParserTest {

    private final ObjectMapper json = new ObjectMapper();
    private final ResponsesOutputParser parser = new ResponsesOutputParser(this.json);

    @Test
    void readsClientToolSearchFunctionCallsTextAndUsage() throws Exception {
        ModelOutputData output = this.parser.toOutput(this.json.readTree("""
                {"output": [
                  {"type": "message", "content": [{"type": "output_text", "text": "Vou olhar sua agenda."}]},
                  {"type": "tool_search_call", "execution": "client", "call_id": "ts1", "arguments": {"goal": "listar eventos"}},
                  {"type": "tool_search_call", "execution": "server", "call_id": "ts2", "arguments": {"paths": ["x"]}},
                  {"type": "function_call", "call_id": "c1", "name": "google_calendar_list_events", "arguments": "{\\"maxResults\\": 5, \\"query\\": null}"}
                 ],
                 "usage": {"input_tokens": 120, "input_tokens_details": {"cached_tokens": 100}, "output_tokens": 9}}
                """));

        assertThat(output.isFinalAnswer()).isFalse();
        assertThat(output.text()).isEqualTo("Vou olhar sua agenda.");
        assertThat(output.toolSearches()).singleElement().satisfies(search -> {
            assertThat(search.callId()).isEqualTo("ts1");
            assertThat(search.goal()).isEqualTo("listar eventos");
        });
        assertThat(output.functionCalls()).singleElement().satisfies(call -> {
            assertThat(call.arguments()).containsEntry("maxResults", 5).containsKey("query");
            assertThat(call.argumentsError()).isNull();
        });
        assertThat(output.usage().cachedInputTokens()).isEqualTo(100);
    }

    @Test
    void invalidJsonArgumentsBecomeAnErrorForTheGatekeeper() throws Exception {
        FunctionCallData call = this.parser.toOutput(this.json.readTree("""
                {"output": [{"type": "function_call", "call_id": "c1", "name": "x", "arguments": "{nao é json"}]}
                """)).functionCalls().getFirst();

        assertThat(call.arguments()).isEmpty();
        assertThat(call.argumentsError()).startsWith("os argumentos não são um JSON válido");
    }

    @Test
    void goalAsStringIsAlsoAccepted() throws Exception {
        ModelOutputData output = this.parser.toOutput(this.json.readTree("""
                {"output": [{"type": "tool_search_call", "execution": "client", "call_id": "t", "arguments": "{\\"goal\\": \\"ler doc\\"}"}]}
                """));

        assertThat(output.toolSearches().getFirst().goal()).isEqualTo("ler doc");
    }
}
