package io.github.caiomc.nexo.adapters.conversation.google;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.caiomc.nexo.adapters.conversation.google.executor.GoogleContextExecutor;
import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolEffect;
import io.github.caiomc.nexo.core.catalog.usecase.ProvideCatalogUseCase;
import io.github.caiomc.nexo.core.conversation.port.tool.ToolExecutorPort;
import io.github.caiomc.nexo.core.conversation.port.tool.command.ExecuteToolCommand;
import io.github.caiomc.nexo.core.conversation.port.tool.data.ToolOutcome;
import io.github.caiomc.nexo.core.conversation.port.tool.data.ToolResultData;
import io.github.caiomc.nexo.core.identity.domain.exception.ConsentRevokedException;
import io.github.caiomc.nexo.core.identity.domain.exception.GoogleCredentialMissingException;
import io.github.caiomc.nexo.core.identity.usecase.ObtainAccessTokenUseCase;
import io.github.caiomc.nexo.core.identity.usecase.command.ObtainAccessTokenCommand;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

import static java.util.Objects.isNull;

/**
 * As mãos do agente: obtém o access token do usuário ({@code ObtainAccessTokenUseCase}) e chama a
 * API REST do Google pelo executor do contexto. Erros viram {@link ToolOutcome}, nunca exceção:
 *
 * <ul>
 *   <li>401: esquece o token em cache e tenta uma vez com um novo;</li>
 *   <li>403 {@code insufficientPermissions}: {@code PERMISSION_DENIED} (o contexto é desligado);</li>
 *   <li>refresh recusado ou sem credencial: {@code RECONNECT_REQUIRED};</li>
 *   <li>outros 4xx/5xx ou argumento inválido: {@code FAILED} com a mensagem.</li>
 * </ul>
 */
public class GoogleToolExecutorAdapter implements ToolExecutorPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleToolExecutorAdapter.class);

    private static final int OUTPUT_LIMIT = 16_000;

    private final Map<ContextKey, GoogleContextExecutor> executors;
    private final ObtainAccessTokenUseCase tokens;
    private final ProvideCatalogUseCase catalog;
    private final GoogleApiClient client;
    private final GoogleApiEndpoints endpoints;
    private final ResultSummarizer summarizer;
    private final ObjectMapper json;

    public GoogleToolExecutorAdapter(
            List<GoogleContextExecutor> executors,
            ObtainAccessTokenUseCase tokens,
            ProvideCatalogUseCase catalog,
            GoogleApiClient client,
            GoogleApiEndpoints endpoints,
            ResultSummarizer summarizer,
            ObjectMapper json
    ) {
        this.executors = executors.stream().collect(Collectors.toUnmodifiableMap(GoogleContextExecutor::context, Function.identity()));
        this.tokens = tokens;
        this.catalog = catalog;
        this.client = client;
        this.endpoints = endpoints;
        this.summarizer = summarizer;
        this.json = json;
    }

    @Override
    public ToolResultData execute(ExecuteToolCommand command) {
        GoogleContextExecutor executor = this.executors.get(command.context());

        if (isNull(executor)) {
            return this.failure(ToolOutcome.FAILED, "Sem executor para o contexto " + command.context());
        }

        ObtainAccessTokenCommand token = new ObtainAccessTokenCommand(command.userId());

        try {
            return this.success(command, this.run(executor, command, token));
        } catch (ConsentRevokedException | GoogleCredentialMissingException e) {
            return this.failure(ToolOutcome.RECONNECT_REQUIRED, e.getMessage());
        } catch (GoogleApiException e) {
            log.info("{} falhou no Google ({} {}): {}", command.tool(), e.status(), e.reason(), e.getMessage());

            return this.failure(e.insufficientPermissions() ? ToolOutcome.PERMISSION_DENIED : ToolOutcome.FAILED, "Google " + e.status() + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return this.failure(ToolOutcome.FAILED, e.getMessage());
        }
    }

    private JsonNode run(
            GoogleContextExecutor executor,
            ExecuteToolCommand command,
            ObtainAccessTokenCommand token
    ) {
        try {
            return executor.execute(this.call(command, this.tokens.obtain(token).value()));
        } catch (GoogleApiException e) {
            if (e.status() != 401) {
                throw e;
            }

            log.debug("Google respondeu 401 para {}: token novo e mais uma tentativa", command.userId());
            this.tokens.invalidate(token);

            return executor.execute(this.call(command, this.tokens.obtain(token).value()));
        }
    }

    private GoogleCall call(
            ExecuteToolCommand command,
            String accessToken
    ) {
        return new GoogleCall(
                command.tool(),
                command.arguments(),
                this.client,
                this.endpoints,
                this.json,
                accessToken
        );
    }

    private ToolResultData success(
            ExecuteToolCommand command,
            JsonNode result
    ) {
        String output = this.write(result);
        boolean truncated = output.length() > OUTPUT_LIMIT;

        return new ToolResultData(
                ToolOutcome.SUCCEEDED,
                truncated ? output.substring(0, OUTPUT_LIMIT) : output,
                this.summarizer.summarize(this.effect(command), result),
                null,
                truncated
        );
    }

    private ToolEffect effect(ExecuteToolCommand command) {
        return this.catalog.current().find(command.tool()).map(Tool::effect).orElse(ToolEffect.READ);
    }

    private ToolResultData failure(
            ToolOutcome outcome,
            String message
    ) {
        return new ToolResultData(
                outcome,
                null,
                null,
                message,
                false
        );
    }

    private String write(JsonNode result) {
        try {
            return this.json.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
