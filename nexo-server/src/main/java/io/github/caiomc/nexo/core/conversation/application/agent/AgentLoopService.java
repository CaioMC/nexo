package io.github.caiomc.nexo.core.conversation.application.agent;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.application.AgentSettings;
import io.github.caiomc.nexo.core.conversation.application.ConversationEventHubService;
import io.github.caiomc.nexo.core.conversation.domain.enums.FailureCode;
import io.github.caiomc.nexo.core.conversation.domain.exception.TurnAbortedException;
import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.domain.policy.ConversationEvents;
import io.github.caiomc.nexo.core.conversation.port.model.ModelSession;
import io.github.caiomc.nexo.core.conversation.port.model.command.ToolOutputCommand;
import io.github.caiomc.nexo.core.conversation.port.model.data.FunctionCallData;
import io.github.caiomc.nexo.core.conversation.port.model.data.ModelOutputData;
import io.github.caiomc.nexo.core.conversation.port.model.data.ToolSearchCallData;

/**
 * O laço do agente (doc 11, seção 3). Cada volta:
 * <ol>
 *     <li>{@link CallModelService}: chama o modelo com as respostas da volta anterior;</li>
 *     <li>resposta final? o turno termina com {@code assistant.message} e {@code turn.completed};</li>
 *     <li>senão, cada {@code tool_search_call} vai ao {@link AnswerToolSearchService} e cada
 *     {@code function_call} ao {@link AnswerFunctionCallService}; as respostas seguem na próxima volta.</li>
 * </ol>
 * A sessão com o modelo é aberta pelo {@link OpenModelSessionService} e fechada ao fim do turno.
 */
@Service
public class AgentLoopService {

    private final OpenModelSessionService openModelSession;
    private final CallModelService callModel;
    private final AnswerToolSearchService answerToolSearch;
    private final AnswerFunctionCallService answerFunctionCall;
    private final RecordTurnStateService recordTurnState;
    private final ConversationEventHubService hub;

    private final ConversationEvents events;

    private final AgentSettings settings;
    private final Clock clock;

    public AgentLoopService(
            OpenModelSessionService openModelSession,
            CallModelService callModel,
            AnswerToolSearchService answerToolSearch,
            AnswerFunctionCallService answerFunctionCall,
            RecordTurnStateService recordTurnState,
            ConversationEventHubService hub,
            ConversationEvents events,
            AgentSettings settings,
            Clock clock
    ) {
        this.openModelSession = openModelSession;
        this.callModel = callModel;
        this.answerToolSearch = answerToolSearch;
        this.answerFunctionCall = answerFunctionCall;
        this.recordTurnState = recordTurnState;
        this.hub = hub;
        this.events = events;
        this.settings = settings;
        this.clock = clock;
    }

    public void runTurn(TurnRun run) {
        try (ModelSession session = this.openModelSession.open(run)) {
            run.attachModelSession(session);
            this.loopUntilFinalAnswer(run, session);
        }
    }

    private void loopUntilFinalAnswer(
            TurnRun run,
            ModelSession session
    ) {
        List<ToolOutputCommand> toolOutputs = List.of();
        int functionCalls = 0;

        while (true) {
            run.ensureCanContinue(this.settings.turnTimeout());

            ModelOutputData output = this.callModel.call(run, session, toolOutputs);

            if (output.isFinalAnswer()) {
                this.completeTurn(run, output.text(), functionCalls);

                return;
            }

            List<ToolOutputCommand> answers = new ArrayList<>();

            for (ToolSearchCallData search : output.toolSearches()) {
                answers.add(this.answerToolSearch.answer(run, search));
            }

            for (FunctionCallData call : output.functionCalls()) {
                functionCalls++;

                if (functionCalls > this.settings.maxToolCalls()) {
                    throw new TurnAbortedException(FailureCode.LIMIT_TOOL_CALLS);
                }

                answers.add(this.answerFunctionCall.answer(run, call));
                run.ensureCanContinue(this.settings.turnTimeout());
            }

            toolOutputs = answers;
        }
    }

    private void completeTurn(
            TurnRun run,
            String text,
            int functionCalls
    ) {
        Turn turn = run.turn();
        Turn completed = turn.completed(text, this.clock.instant());

        this.hub.publish(turn.conversationId(), this.events.assistantMessage(turn.id(), text));
        this.recordTurnState.record(run, completed);
        this.hub.publish(
                turn.conversationId(),
                this.events.turnCompleted(
                        turn.id(),
                        run.runningTime().toMillis(),
                        functionCalls,
                        completed.usage()
                )
        );
    }
}
