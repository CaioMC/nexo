package io.github.caiomc.nexo.it.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static java.util.Objects.isNull;

/**
 * Um servidor HTTP local no lugar de tudo que fica fora do Nexo: a Responses API (SSE com respostas
 * roteirizadas), o endpoint de token do Google e as APIs REST do Google.
 */
public final class FakeUpstream implements AutoCloseable {

    private final HttpServer server;
    private final ConcurrentLinkedDeque<String> modelScript = new ConcurrentLinkedDeque<>();
    private final Map<String, String> google = new ConcurrentHashMap<>();

    public final List<String> modelRequests = new CopyOnWriteArrayList<>();
    public final List<String> googleRequests = new CopyOnWriteArrayList<>();

    public FakeUpstream() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/v1/responses", this::model);
        this.server.createContext("/token", exchange -> this.reply(exchange, 200, "{\"access_token\":\"google-token\",\"expires_in\":3600,\"scope\":\"https://www.googleapis.com/auth/calendar\"}"));
        this.server.createContext("/revoke", exchange -> this.reply(exchange, 200, "{}"));
        this.server.createContext("/google", this::google);
        this.server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + this.server.getAddress().getPort();
    }

    /**
     * Próximas respostas do modelo, em ordem (uma por chamada a {@code /v1/responses}).
     */
    public void script(String... responses) {
        this.modelScript.clear();
        this.modelScript.addAll(List.of(responses));
        this.modelRequests.clear();
    }

    /**
     * Resposta de uma rota do Google, ex.: {@code "GET /google/calendar/users/me/calendarList"}.
     */
    public void google(
            String route,
            String json
    ) {
        this.google.put(route, json);
    }

    public static String toolSearch(
            String callId,
            String goal
    ) {
        return completed("[{\"type\":\"tool_search_call\",\"execution\":\"client\",\"call_id\":\"" + callId + "\",\"status\":\"completed\",\"arguments\":{\"goal\":\"" + goal + "\"}}]");
    }

    public static String functionCall(
            String callId,
            String name,
            String argumentsJson
    ) {
        String arguments = argumentsJson.replace("\\", "\\\\").replace("\"", "\\\"");

        return completed("[{\"type\":\"function_call\",\"call_id\":\"" + callId + "\",\"name\":\"" + name + "\",\"arguments\":\"" + arguments + "\",\"status\":\"completed\"}]");
    }

    /**
     * Texto final em streaming: um evento de delta por palavra.
     */
    public static String finalText(String text) {
        StringBuilder sse = new StringBuilder();

        for (String word : text.split("(?<= )")) {
            sse.append("data: {\"type\":\"response.output_text.delta\",\"delta\":\"").append(word).append("\"}\n\n");
        }

        return sse + completed("[{\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"" + text + "\"}]}]");
    }

    private static String completed(String output) {
        return "data: {\"type\":\"response.completed\",\"response\":{\"output\":" + output + ",\"usage\":{\"input_tokens\":100,\"input_tokens_details\":{\"cached_tokens\":40},\"output_tokens\":20}}}\n\n";
    }

    private void model(HttpExchange exchange) throws IOException {
        this.modelRequests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        String next = this.modelScript.poll();

        if (isNull(next)) {
            this.reply(exchange, 500, "{\"error\":{\"message\":\"roteiro do modelo acabou\"}}");

            return;
        }

        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(next.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void google(HttpExchange exchange) throws IOException {
        String route = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        this.googleRequests.add(route + " " + body);

        String json = this.google.get(route);

        if (isNull(json)) {
            this.reply(exchange, 404, "{\"error\":{\"code\":404,\"message\":\"rota falsa sem resposta: " + route + "\"}}");

            return;
        }

        this.reply(exchange, 200, json);
    }

    private void reply(
            HttpExchange exchange,
            int status,
            String json
    ) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        this.server.stop(0);
    }
}
