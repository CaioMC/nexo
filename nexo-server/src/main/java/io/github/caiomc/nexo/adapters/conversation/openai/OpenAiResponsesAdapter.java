package io.github.caiomc.nexo.adapters.conversation.openai;

import io.github.caiomc.nexo.adapters.conversation.openai.mapper.ResponsesOutputParser;
import io.github.caiomc.nexo.adapters.conversation.openai.mapper.ResponsesRequestMapper;
import io.github.caiomc.nexo.adapters.shared.openai.OpenAiHttpClient;
import io.github.caiomc.nexo.adapters.shared.openai.OpenAiSettings;
import io.github.caiomc.nexo.core.conversation.port.model.LanguageModelPort;
import io.github.caiomc.nexo.core.conversation.port.model.ModelSession;
import io.github.caiomc.nexo.core.conversation.port.model.command.OpenModelSessionCommand;

/**
 * {@link LanguageModelPort} pela Responses API (OpenAI ou o proxy da DTA), em streaming.
 */
public class OpenAiResponsesAdapter implements LanguageModelPort {

    private final OpenAiHttpClient http;
    private final OpenAiSettings settings;
    private final ResponsesRequestMapper requests;
    private final ResponsesOutputParser parser;

    public OpenAiResponsesAdapter(
            OpenAiHttpClient http,
            OpenAiSettings settings,
            ResponsesRequestMapper requests,
            ResponsesOutputParser parser
    ) {
        this.http = http;
        this.settings = settings;
        this.requests = requests;
        this.parser = parser;
    }

    @Override
    public ModelSession openSession(OpenModelSessionCommand command) {
        return new OpenAiResponsesSession(
                new ResponsesStreamer(this.http),
                this.requests,
                this.parser,
                this.settings.model(),
                command
        );
    }

    @Override
    public boolean isConfigured() {
        return this.http.configured();
    }

    @Override
    public String modelName() {
        return this.settings.model();
    }
}
