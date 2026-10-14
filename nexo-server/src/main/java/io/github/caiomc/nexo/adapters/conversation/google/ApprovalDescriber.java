package io.github.caiomc.nexo.adapters.conversation.google;

import java.util.List;
import java.util.Map;

import io.github.caiomc.nexo.core.conversation.port.approval.ApprovalDescriberPort;
import io.github.caiomc.nexo.core.conversation.port.approval.command.DescribeApprovalCommand;

import static java.util.Objects.nonNull;

/**
 * A frase do cartão de aprovação, montada pelo servidor a partir dos argumentos (doc 11, seção 5).
 */
public class ApprovalDescriber implements ApprovalDescriberPort {

    @Override
    public String describe(DescribeApprovalCommand command) {
        Map<String, Object> args = command.arguments();

        return switch (command.tool().value()) {
            case "google_gmail_create_draft" -> "Criar rascunho de e-mail para " + this.join(args, "to") + " com assunto “" + this.text(args, "subject") + "”";
            case "google_gmail_modify_message" -> "Mudar etiquetas da mensagem " + this.text(args, "messageId") + this.labels(args);
            case "google_gmail_modify_thread" -> "Mudar etiquetas da conversa " + this.text(args, "threadId") + this.labels(args);
            case "google_drive_create_file" -> "Criar no Drive o arquivo “" + this.text(args, "name") + "”";
            case "google_drive_update_content" -> "Substituir todo o conteúdo do arquivo “" + this.text(args, "name", "fileId") + "”";
            case "google_drive_rename_file" -> "Renomear o arquivo " + this.text(args, "fileId") + " para “" + this.text(args, "name") + "”";
            case "google_drive_copy_file" -> "Copiar o arquivo " + this.text(args, "fileId") + " como “" + this.text(args, "name") + "”";
            case "google_drive_trash_file" -> "Mover “" + this.text(args, "name", "fileId") + "” para a lixeira do Drive";
            case "google_drive_delete_file" -> "Apagar para sempre “" + this.text(args, "name", "fileId") + "” (não dá para desfazer)";
            case "google_docs_create" -> "Criar o documento “" + this.text(args, "title") + "”";
            case "google_docs_replace_text" -> "Trocar “" + this.text(args, "find") + "” por “" + this.text(args, "replace") + "” no documento " + this.text(args, "documentId");
            case "google_docs_append_text" -> "Acrescentar texto no fim do documento " + this.text(args, "documentId");
            case "google_docs_batch_update" -> "Aplicar alterações em lote no documento " + this.text(args, "documentId");
            case "google_sheets_create" -> "Criar a planilha “" + this.text(args, "title") + "”";
            case "google_sheets_update_values" -> "Escrever " + this.size(args, "values") + " linha(s) em " + this.text(args, "range") + " da planilha " + this.text(args, "spreadsheetId");
            case "google_sheets_clear_values" -> "Limpar o intervalo " + this.text(args, "range") + " da planilha " + this.text(args, "spreadsheetId");
            case "google_sheets_batch_update" -> "Aplicar alterações em lote na planilha " + this.text(args, "spreadsheetId");
            case "google_slides_create" -> "Criar a apresentação “" + this.text(args, "title") + "”";
            case "google_slides_replace_text" -> "Trocar “" + this.text(args, "find") + "” por “" + this.text(args, "replace") + "” na apresentação " + this.text(args, "presentationId");
            case "google_slides_batch_update" -> "Aplicar alterações em lote na apresentação " + this.text(args, "presentationId");
            case "google_calendar_create_event" -> "Criar o evento “" + this.text(args, "summary") + "” de " + this.text(args, "start") + " a " + this.text(args, "end") + this.attendees(args);
            case "google_calendar_update_event" -> "Alterar o evento " + this.text(args, "eventId");
            case "google_calendar_respond_to_event" -> "Responder “" + this.text(args, "responseStatus") + "” ao evento " + this.text(args, "eventId");
            case "google_calendar_delete_event" -> "Apagar o evento “" + this.text(args, "summary", "eventId") + "” e avisar os convidados";
            case "google_chat_send_message" -> "Enviar no Chat para " + this.text(args, "space") + ": “" + this.text(args, "text") + "”";
            case "google_chat_mark_read" -> "Marcar o espaço " + this.text(args, "space") + " como lido";
            case "google_chat_mark_unread" -> "Marcar o espaço " + this.text(args, "space") + " como não lido";
            case "google_people_create_contact" -> "Criar o contato “" + (this.text(args, "givenName") + " " + this.text(args, "familyName")).strip() + "”";
            case "google_people_update_contact" -> "Alterar o contato " + this.text(args, "resourceName");
            case "google_people_delete_contact" -> "Apagar o contato “" + this.text(args, "name", "resourceName") + "”";
            default -> "Executar " + command.tool();
        };
    }

    private String text(
            Map<String, Object> args,
            String... names
    ) {
        for (String name : names) {
            Object value = args.get(name);

            if (nonNull(value) && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }

        return "";
    }

    private String join(
            Map<String, Object> args,
            String name
    ) {
        return args.get(name) instanceof List<?> values && !values.isEmpty() ? String.join(", ", values.stream().map(String::valueOf).toList()) : "(sem destinatário)";
    }

    private int size(
            Map<String, Object> args,
            String name
    ) {
        return args.get(name) instanceof List<?> values ? values.size() : 0;
    }

    private String labels(Map<String, Object> args) {
        return " (adicionar: " + this.join(args, "addLabelIds") + "; remover: " + this.join(args, "removeLabelIds") + ")";
    }

    private String attendees(Map<String, Object> args) {
        return args.get("attendees") instanceof List<?> values && !values.isEmpty() ? ", convidando " + this.join(args, "attendees") : "";
    }
}
