package io.github.caiomc.nexo.adapters.conversation.google;

import static java.util.Objects.isNull;

/**
 * URLs base das APIs do Google. Em teste, todas apontam para um servidor HTTP local.
 */
public record GoogleApiEndpoints(
        String gmail,
        String drive,
        String driveUpload,
        String docs,
        String sheets,
        String slides,
        String calendar,
        String chat,
        String people
) {

    public static GoogleApiEndpoints defaults() {
        return new GoogleApiEndpoints(
                "https://gmail.googleapis.com/gmail/v1/users/me",
                "https://www.googleapis.com/drive/v3",
                "https://www.googleapis.com/upload/drive/v3",
                "https://docs.googleapis.com/v1/documents",
                "https://sheets.googleapis.com/v4/spreadsheets",
                "https://slides.googleapis.com/v1/presentations",
                "https://www.googleapis.com/calendar/v3",
                "https://chat.googleapis.com/v1",
                "https://people.googleapis.com/v1"
        );
    }

    /**
     * Todas as APIs sob uma URL só ({@code nexo.google.api-base}), com um prefixo por API.
     */
    public static GoogleApiEndpoints under(String base) {
        if (isNull(base) || base.isBlank()) {
            return defaults();
        }

        String root = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;

        return new GoogleApiEndpoints(
                root + "/gmail",
                root + "/drive",
                root + "/upload/drive",
                root + "/docs",
                root + "/sheets",
                root + "/slides",
                root + "/calendar",
                root + "/chat",
                root + "/people"
        );
    }
}
