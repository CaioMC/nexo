/**
 * Envelope do WebSocket (doc 06). Tudo que o servidor conta vira `event` com `seq`; comandos do
 * cliente recebem `ack` ou `nack`.
 */

export type EventType =
  | 'message.user'
  | 'turn.started'
  | 'assistant.delta'
  | 'assistant.message'
  | 'tool.search'
  | 'context.suggested'
  | 'tool.started'
  | 'tool.blocked'
  | 'approval.requested'
  | 'approval.resolved'
  | 'tool.completed'
  | 'usage.updated'
  | 'turn.completed'
  | 'turn.failed'
  | 'context.changed'
  | 'conversation.renamed'

export interface EventEnvelope {
  kind: 'event'
  seq: number
  conversationId: string
  turnId: string | null
  type: EventType
  at: string
  payload: Record<string, unknown>
}

export interface AckMessage {
  kind: 'ack'
  commandId: string
  turnId?: string
}

export interface NackMessage {
  kind: 'nack'
  commandId?: string
  code: string
  message: string
}

export type ServerMessage = EventEnvelope | AckMessage | NackMessage

export type CommandType = 'user.message' | 'approval.decide' | 'turn.stop'

export interface Command {
  kind: 'command'
  id: string
  type: CommandType
  payload: Record<string, unknown>
}

export interface Usage {
  modelCalls: number
  inputTokens: number
  cachedInputTokens: number
  outputTokens: number
}

export interface SelectedTool {
  step: number
  name: string
  origin: 'RAG' | 'DAG'
  score: number
  level: number | null
}

export interface BlockedTool {
  name: string
  missingContexts: string[]
}

export type FailureCode =
  | 'STOPPED'
  | 'LIMIT_MODEL_CALLS'
  | 'LIMIT_TOOL_CALLS'
  | 'TIMEOUT'
  | 'MODEL_ERROR'
  | 'GOOGLE_RECONNECT_REQUIRED'
  | 'SERVER_RESTARTED'
