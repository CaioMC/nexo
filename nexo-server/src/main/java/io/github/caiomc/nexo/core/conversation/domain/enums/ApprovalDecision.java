package io.github.caiomc.nexo.core.conversation.domain.enums;

import java.util.Locale;

import static java.util.Objects.isNull;

/**
 * O que o usuário respondeu no cartão de aprovação.
 */
public enum ApprovalDecision {

    APPROVE,
    REJECT;

    public static ApprovalDecision of(String value) {
        if (isNull(value)) {
            throw new IllegalArgumentException("Informe a decisão: approve ou reject");
        }

        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "approve" -> APPROVE;
            case "reject" -> REJECT;
            default -> throw new IllegalArgumentException("Decisão desconhecida: '" + value + "'. Use approve ou reject.");
        };
    }

    public ApprovalStatus status() {
        return this == APPROVE ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
    }
}
