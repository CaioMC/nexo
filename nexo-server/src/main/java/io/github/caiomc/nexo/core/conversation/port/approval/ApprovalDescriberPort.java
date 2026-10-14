package io.github.caiomc.nexo.core.conversation.port.approval;

import io.github.caiomc.nexo.core.conversation.port.approval.command.DescribeApprovalCommand;

/**
 * Descrição legível de uma alteração, gerada pelo servidor a partir dos argumentos: o modelo não
 * consegue disfarçar uma exclusão de "pequeno ajuste".
 */
public interface ApprovalDescriberPort {

    String describe(DescribeApprovalCommand command);
}
