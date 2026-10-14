package io.github.caiomc.nexo.adapters.conversation.scheduling;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.conversation.usecase.FailInterruptedTurnsUseCase;

/**
 * Na subida, encerra os turnos que ficaram ativos quando o servidor parou (RNF-08).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class TurnRecovery implements ApplicationRunner {

    private final FailInterruptedTurnsUseCase recover;

    public TurnRecovery(FailInterruptedTurnsUseCase recover) {
        this.recover = recover;
    }

    @Override
    public void run(ApplicationArguments args) {
        this.recover.recover();
    }
}
