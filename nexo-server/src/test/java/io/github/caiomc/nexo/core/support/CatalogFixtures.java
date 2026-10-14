package io.github.caiomc.nexo.core.support;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.github.caiomc.nexo.core.catalog.domain.model.DependencyGraph;
import io.github.caiomc.nexo.core.catalog.domain.model.Tool;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolCatalog;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolEffect;
import io.github.caiomc.nexo.core.catalog.domain.model.ToolName;
import io.github.caiomc.nexo.core.shared.domain.model.ContextKey;

/**
 * Catálogo sintético e pequeno para os testes do core, com uma dependência entre contextos
 * (docs_summarize_shared depende de drive_get_metadata) que o seed real não tem.
 *
 * <pre>
 * calendar_create_event → calendar_list_events → calendar_list_calendars
 * calendar_delete_event → calendar_get_event  → calendar_list_events
 * docs_replace_text     → docs_read
 * docs_summarize_shared → docs_read, drive_get_metadata
 * drive_get_metadata
 * gmail_search
 * </pre>
 */
public final class CatalogFixtures {

    public static final ContextKey CALENDAR = ContextKey.of("calendar");
    public static final ContextKey DOCS = ContextKey.of("docs");
    public static final ContextKey DRIVE = ContextKey.of("drive");
    public static final ContextKey GMAIL = ContextKey.of("gmail");

    private CatalogFixtures() {
    }

    public static ToolName name(String value) {
        return ToolName.of(value);
    }

    public static List<ToolName> names(String... values) {
        return Arrays.stream(values).map(ToolName::of).toList();
    }

    public static Tool tool(
            String name,
            ContextKey context,
            ToolEffect effect
    ) {
        String namespace = context.value() + "_ns";
        String description = "Ferramenta " + name.replace('_', ' ');

        return new Tool(
                ToolName.of(name),
                namespace,
                context,
                effect,
                description,
                List.of()
        );
    }

    public static Map<ToolName, List<ToolName>> edges() {
        return Map.of(
                name("calendar_create_event"), names("calendar_list_events"),
                name("calendar_list_events"), names("calendar_list_calendars"),
                name("calendar_delete_event"), names("calendar_get_event"),
                name("calendar_get_event"), names("calendar_list_events"),
                name("docs_replace_text"), names("docs_read"),
                name("docs_summarize_shared"), names("docs_read", "drive_get_metadata")
        );
    }

    public static List<Tool> tools() {
        return List.of(
                tool("calendar_list_calendars", CALENDAR, ToolEffect.READ),
                tool("calendar_list_events", CALENDAR, ToolEffect.READ),
                tool("calendar_get_event", CALENDAR, ToolEffect.READ),
                tool("calendar_create_event", CALENDAR, ToolEffect.WRITE),
                tool("calendar_delete_event", CALENDAR, ToolEffect.DESTRUCTIVE),
                tool("docs_read", DOCS, ToolEffect.READ),
                tool("docs_replace_text", DOCS, ToolEffect.WRITE),
                tool("docs_summarize_shared", DOCS, ToolEffect.READ),
                tool("drive_get_metadata", DRIVE, ToolEffect.READ),
                tool("gmail_search", GMAIL, ToolEffect.READ)
        );
    }

    public static DependencyGraph graph() {
        return new DependencyGraph(
                tools().stream().map(Tool::name).toList(),
                edges()
        );
    }

    public static ToolCatalog catalog() {
        return new ToolCatalog(tools(), graph());
    }
}
