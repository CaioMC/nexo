package io.github.caiomc.nexo.core.identity.domain.model;

/**
 * Resultado do login.
 *
 * @param offlineAccess há refresh token guardado; se não houver, o login é refeito uma vez com
 *                      {@code prompt=consent} para o Google emitir um
 */
public record SignIn(
        User user,
        boolean offlineAccess
) {
}
