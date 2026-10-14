package io.github.caiomc.nexo.core.conversation.application.agent;

import org.springframework.stereotype.Service;

import io.github.caiomc.nexo.core.conversation.domain.model.Turn;
import io.github.caiomc.nexo.core.conversation.mapper.TurnDataMapper;
import io.github.caiomc.nexo.core.conversation.port.repository.TurnRepository;

/**
 * Cada mudança de estado do turno (consumo, esperando aprovação, concluído, falhou) passa por aqui:
 * vale para o turno em memória e é gravada no banco no mesmo passo.
 */
@Service
public class RecordTurnStateService {

    private final TurnRepository turns;
    private final TurnDataMapper mapper;

    public RecordTurnStateService(
            TurnRepository turns,
            TurnDataMapper mapper
    ) {
        this.turns = turns;
        this.mapper = mapper;
    }

    public void record(
            TurnRun run,
            Turn current
    ) {
        run.updateTurn(current);
        this.turns.update(this.mapper.toData(current));
    }
}
