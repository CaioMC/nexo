package io.github.caiomc.nexo.adapters.conversation.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.conversation.usecase.ExpireApprovalsUseCase;

/**
 * A cada 30 s, vence as aprovações pendentes fora do prazo (RN-06).
 */
@Component
public class ApprovalExpirer {

    private final ExpireApprovalsUseCase expire;

    public ApprovalExpirer(ExpireApprovalsUseCase expire) {
        this.expire = expire;
    }

    @Scheduled(fixedDelayString = "${nexo.agent.approval-sweep:30s}", initialDelayString = "${nexo.agent.approval-sweep:30s}")
    public void sweep() {
        this.expire.expire();
    }
}
