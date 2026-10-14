package io.github.caiomc.nexo.adapters.conversation.config;

import java.time.ZoneId;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.adapters.conversation.google.ApprovalDescriber;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleApiClient;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleApiEndpoints;
import io.github.caiomc.nexo.adapters.conversation.google.GoogleToolExecutorAdapter;
import io.github.caiomc.nexo.adapters.conversation.google.ResultSummarizer;
import io.github.caiomc.nexo.adapters.conversation.google.executor.CalendarExecutor;
import io.github.caiomc.nexo.adapters.conversation.google.executor.ChatExecutor;
import io.github.caiomc.nexo.adapters.conversation.google.executor.DocsExecutor;
import io.github.caiomc.nexo.adapters.conversation.google.executor.DriveExecutor;
import io.github.caiomc.nexo.adapters.conversation.google.executor.GmailExecutor;
import io.github.caiomc.nexo.adapters.conversation.google.executor.PeopleExecutor;
import io.github.caiomc.nexo.adapters.conversation.google.executor.SheetsExecutor;
import io.github.caiomc.nexo.adapters.conversation.google.executor.SlidesExecutor;
import io.github.caiomc.nexo.adapters.conversation.openai.OpenAiResponsesAdapter;
import io.github.caiomc.nexo.adapters.conversation.openai.mapper.ResponsesOutputParser;
import io.github.caiomc.nexo.adapters.conversation.openai.mapper.ResponsesRequestMapper;
import io.github.caiomc.nexo.adapters.conversation.openai.mapper.ResponsesToolMapper;
import io.github.caiomc.nexo.adapters.conversation.websocket.handler.ConversationWebSocketHandler;
import io.github.caiomc.nexo.adapters.conversation.websocket.mapper.EventMessageMapper;
import io.github.caiomc.nexo.adapters.shared.openai.OpenAiHttpClient;
import io.github.caiomc.nexo.adapters.shared.openai.OpenAiSettings;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.conversation.application.AgentSettings;
import io.github.caiomc.nexo.core.conversation.domain.policy.ArgumentsHasher;
import io.github.caiomc.nexo.core.conversation.domain.policy.ArgumentsValidator;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.domain.policy.InstructionsBuilder;
import io.github.caiomc.nexo.core.conversation.port.approval.ApprovalDescriberPort;
import io.github.caiomc.nexo.core.conversation.port.model.LanguageModelPort;
import io.github.caiomc.nexo.core.conversation.port.tool.ToolExecutorPort;
import io.github.caiomc.nexo.core.conversation.usecase.DecideApprovalUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.SendMessageUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.StopTurnUseCase;
import io.github.caiomc.nexo.core.conversation.usecase.WatchConversationUseCase;
import io.github.caiomc.nexo.core.identity.usecase.ObtainAccessTokenUseCase;

import static java.util.Objects.isNull;

@Configuration
@EnableScheduling
public class ConversationConfig {

    @Bean
    AgentSettings agentSettings(AgentProperties agent) {
        return new AgentSettings(
                agent.maxModelCalls(),
                agent.maxToolCalls(),
                agent.turnTimeout(),
                agent.approvalTtl(),
                agent.prerequisiteWindow(),
                agent.historyTurns(),
                agent.toolMemoryTurns(),
                agent.deltaWindow(),
                agent.exposure(),
                isNull(agent.defaultTimeZone()) ? null : ZoneId.of(agent.defaultTimeZone())
        );
    }

    @Bean
    ConversationEvents conversationEvents() {
        return new ConversationEvents();
    }

    @Bean
    ArgumentsValidator argumentsValidator() {
        return new ArgumentsValidator();
    }

    @Bean
    ArgumentsHasher argumentsHasher() {
        return new ArgumentsHasher();
    }

    @Bean
    InstructionsBuilder instructionsBuilder() {
        return new InstructionsBuilder();
    }

    @Bean
    LanguageModelPort languageModel(
            OpenAiHttpClient http,
            OpenAiSettings settings,
            ProvideCatalogUseCase catalog,
            ObjectMapper json
    ) {
        return new OpenAiResponsesAdapter(
                http,
                settings,
                new ResponsesRequestMapper(json, new ResponsesToolMapper(json, catalog)),
                new ResponsesOutputParser(json)
        );
    }

    @Bean
    ToolExecutorPort googleToolExecutor(
            ObtainAccessTokenUseCase tokens,
            ProvideCatalogUseCase catalog,
            AgentProperties agent,
            ObjectMapper json
    ) {
        return new GoogleToolExecutorAdapter(
                List.of(
                        new GmailExecutor(),
                        new DriveExecutor(),
                        new DocsExecutor(),
                        new SheetsExecutor(),
                        new SlidesExecutor(),
                        new CalendarExecutor(),
                        new ChatExecutor(),
                        new PeopleExecutor()
                ),
                tokens,
                catalog,
                new GoogleApiClient(json),
                GoogleApiEndpoints.under(agent.googleApiBase()),
                new ResultSummarizer(),
                json
        );
    }

    @Bean
    ApprovalDescriberPort approvalDescriber() {
        return new ApprovalDescriber();
    }

    @Bean
    ConversationWebSocketHandler conversationWebSocketHandler(
            WatchConversationUseCase watch,
            SendMessageUseCase send,
            DecideApprovalUseCase decide,
            StopTurnUseCase stop,
            EventMessageMapper mapper,
            ObjectMapper json
    ) {
        return new ConversationWebSocketHandler(
                watch,
                send,
                decide,
                stop,
                mapper,
                json
        );
    }
}
