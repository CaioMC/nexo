package io.github.caiomc.nexo.adapters.identity.web.response;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        String pictureUrl
) {
}
