package io.github.caiomc.nexo.core.conversation.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolCallStatus;
import io.github.caiomc.nexo.core.conversation.domain.enums.ToolExposure;
import io.github.caiomc.nexo.core.conversation.domain.model.ToolCall;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.port.model.command.HistoryMessageCommand;
import io.github.caiomc.nexo.core.conversation.port.model.command.ModelCallCommand;
import io.github.caiomc.nexo.core.conversation.port.model.command.OpenModelSessionCommand;
import io.github.caiomc.nexo.core.conversation.port.model.command.ToolOutputCommand;

import static java.util.Objects.isNull;

/**
 * Tudo o que vai ao {@code LanguageModelPort}: a abertura da sessão (com o histórico, doc 11,
 * seção 7), cada chamada e as respostas aos pedidos do modelo. O texto das respostas de erro fica
 * no {@link FunctionOutputMapper}.
 */
@Component
public class ModelCommandMapper {

    public OpenModelSessionCommand toOpenSession(
            String instructions,
            List<HistoryMessageCommand> history,
            Turn turn,
            ToolExposure exposure,
            List<Tool> preloadedTools,
            List<String> namespaces
    ) {
        return new OpenModelSessionCommand(
                instructions,
                history,
                turn.userMessage(),
                exposure,
                preloadedTools,
                namespaces
        );
    }

    /**
     * Turnos anteriores, do mais antigo ao mais novo: a mensagem do usuário e a resposta final,
     * com o resumo das ferramentas usadas (não a saída completa).
     */
    public List<HistoryMessageCommand> toHistory(
            List<Turn> previousTurns,
            Map<UUID, List<ToolCall>> callsByTurn
    ) {
        List<HistoryMessageCommand> history = new ArrayList<>();

        for (Turn turn : previousTurns) {
            history.add(new HistoryMessageCommand(HistoryMessageCommand.USER, turn.userMessage()));
            history.add(new HistoryMessageCommand(HistoryMessageCommand.ASSISTANT, this.assistantAnswer(turn, callsByTurn.getOrDefault(turn.id(), List.of()))));
        }

        return history;
    }

    public ModelCallCommand toCall(List<ToolOutputCommand> toolOutputs) {
        return new ModelCallCommand(toolOutputs);
    }

    public ToolOutputCommand toToolSearchOutput(
            String callId,
            List<Tool> tools
    ) {
        return new ToolOutputCommand.SearchResult(callId, tools);
    }

    public ToolOutputCommand toFunctionOutput(
            String callId,
            String output
    ) {
        return new ToolOutputCommand.FunctionResult(callId, output);
    }

    private String assistantAnswer(
            Turn turn,
            List<ToolCall> calls
    ) {
        String text = isNull(turn.assistantMessage()) ? "(o turno terminou sem resposta: " + turn.failure() + ")" : turn.assistantMessage();
        String tools = calls.stream()
                .filter(call -> call.status() == ToolCallStatus.SUCCEEDED)
                .map(call -> call.tool().value() + (isNull(call.outputSummary()) ? "" : " → " + call.outputSummary()))
                .collect(Collectors.joining("; "));

        return tools.isEmpty() ? text : "[Ferramentas usadas: " + tools + "]\n" + text;
    }
}
