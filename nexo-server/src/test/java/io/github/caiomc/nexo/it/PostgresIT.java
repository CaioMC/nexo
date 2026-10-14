package io.github.caiomc.nexo.it;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.github.caiomc.nexo.adapters.identity.security.SessionUser;

/**
 * Base dos testes de integração: o app inteiro contra um PostgreSQL 17 com pgvector de verdade,
 * com as migrations do Flyway, num servidor de verdade em porta aleatória (o WebSocket precisa de
 * um container). Sem chave de modelo (nada vai para a rede) e sem indexar na subida.
 *
 * <p>Container único para todas as classes (iniciado uma vez, removido pelo Ryuk no fim): o
 * contexto Spring fica em cache entre as classes e precisa continuar apontando para o mesmo banco.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "nexo.openai.api-key=",
        "nexo.retrieval.index-on-startup=false"
})
public abstract class PostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres")
    );

    static {
        POSTGRES.start();
    }

    /**
     * Um usuário como o login com Google deixaria: linha em {@code app_user}, sem contextos ligados.
     */
    protected static SessionUser newUser(
            JdbcClient jdbc,
            String email
    ) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());

        jdbc.sql("INSERT INTO app_user (id, google_subject, email, display_name, created_at, last_login_at) VALUES (:id, :subject, :email, 'Teste', :now, :now)")
                .param("id", id)
                .param("subject", "sub-" + id)
                .param("email", email)
                .param("now", now)
                .update();

        return new SessionUser(id, email);
    }

    protected static void enable(
            JdbcClient jdbc,
            SessionUser user,
            String... contexts
    ) {
        for (String context : contexts) {
            jdbc.sql("INSERT INTO user_context (user_id, context_key, enabled, changed_at) VALUES (:user, :context, true, now()) ON CONFLICT (user_id, context_key) DO UPDATE SET enabled = true")
                    .param("user", user.userId())
                    .param("context", context)
                    .update();
        }
    }
}
