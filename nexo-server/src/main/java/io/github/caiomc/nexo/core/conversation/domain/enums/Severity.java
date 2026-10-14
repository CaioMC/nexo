package io.github.caiomc.nexo.core.conversation.domain.enums;

import java.util.Locale;

import io.github.caiomc.nexo.core.catalog.domain.model.ToolEffect;

/**
 * RN-07: {@code WRITE} → {@code warning}; {@code DESTRUCTIVE} → {@code danger} (o front pede um
 * segundo clique).
 */
public enum Severity {

    WARNING,
    DANGER;

    public static Severity of(ToolEffect effect) {
        return effect == ToolEffect.DESTRUCTIVE ? DANGER : WARNING;
    }

    public static Severity fromWire(String value) {
        return Severity.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public String wire() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
