package io.github.caiomc.nexo.core.identity.port.repository.data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Uma linha de {@code google_credential}.
 */
public record GoogleCredentialData(
        UUID userId,
        byte[] refreshToken,
        int keyVersion,
        List<String> grantedScopes,
        Instant updatedAt
) {

    @Override
    public String toString() {
        return "GoogleCredentialData[userId=" + this.userId + ", refreshToken=***, keyVersion=" + this.keyVersion + "]";
    }
}
