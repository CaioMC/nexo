package io.github.caiomc.nexo.core.conversation.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.conversation.domain.enums.BlockReason;
import io.github.caiomc.nexo.core.conversation.domain.model.Admission;
import io.github.caiomc.nexo.core.conversation.domain.policy.ArgumentsHasher;

/**
 * O que volta ao modelo quando uma {@code function_call} não rodou ou falhou:
 * {@code {"error": código, "message": texto}}. A mensagem diz ao modelo o que fazer em seguida.
 */
@Component
public class FunctionOutputMapper {

    private final ArgumentsHasher json;

    public FunctionOutputMapper(ArgumentsHasher json) {
        this.json = json;
    }

    public String toUnknownTool(Admission.Unknown unknown) {
        return this.toError("unknown_tool", unknown.message());
    }

    public String toInvalidArguments(Admission.Invalid invalid) {
        return this.toError("invalid_arguments", "Argumentos inválidos para " + invalid.tool().name() + ": " + String.join("; ", invalid.errors()));
    }

    public String toBlocked(Admission.Blocked blocked) {
        return blocked.reason() == BlockReason.CONTEXT_DISABLED ?
                this.toError("context_disabled", "O contexto " + String.join(", ", blocked.missing()) + " está desligado. Peça ao usuário para ligá-lo na tela de contextos.") :
                this.toError("missing_prerequisites", "Execute antes: " + String.join(", ", blocked.missing()));
    }

    public String toApprovalExpired() {
        return this.toError("approval_expired", "O usuário não respondeu a tempo; a alteração não foi feita.");
    }

    public String toApprovalRejected() {
        return this.toError("approval_rejected", "O usuário recusou a alteração; ela não foi feita. Pergunte o que ele prefere.");
    }

    public String toToolFailed(String error) {
        return this.toError("tool_failed", error);
    }

    public String toPermissionDenied(
            Tool tool,
            String error
    ) {
        return this.toError("permission_denied", "O Google negou acesso ao contexto " + tool.context() + " (" + error + "). Ele foi desligado; peça ao usuário para ligá-lo de novo.");
    }

    private String toError(
            String code,
            String message
    ) {
        Map<String, Object> error = new LinkedHashMap<>();

        error.put("error", code);
        error.put("message", message);

        return this.json.canonical(error);
    }
}
