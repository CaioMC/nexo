import type { BlockedTool, EventEnvelope, SelectedTool, Usage } from './events'

/**
 * A timeline da conversa montada a partir dos eventos (doc 12, seção 3). O reducer é puro e
 * idempotente: evento com `seq` já aplicado é ignorado, então o replay depois de reconectar não
 * duplica nada.
 */

export type ToolCallStatus = 'running' | 'ok' | 'error' | 'blocked'
export type ApprovalStatus = 'pending' | 'approved' | 'rejected' | 'expired'

export type TimelineItem =
  | { kind: 'user'; seq: number; text: string }
  | { kind: 'assistant'; seq: number; text: string; streaming: boolean }
  | { kind: 'toolSearch'; seq: number; goal: string; strategy: string; tools: SelectedTool[]; blocked: BlockedTool[]; durationMs: number }
  | {
      kind: 'toolCall'
      seq: number
      callId: string
      tool: string
      effect?: string
      status: ToolCallStatus
      summary?: string
      error?: string
      reason?: string
      missing?: string[]
      arguments?: unknown
    }
  | {
      kind: 'approval'
      seq: number
      approvalId: string
      tool: string
      status: ApprovalStatus
      severity: 'warning' | 'danger'
      description: string
      args: unknown
      expiresAt: string
    }
  | { kind: 'contextSuggestion'; seq: number; contexts: string[] }
  | { kind: 'turnEnd'; seq: number; ok: boolean; code?: string; message?: string; durationMs?: number }

export interface ConversationState {
  lastSeq: number
  items: TimelineItem[]
  activeTurn?: string
  usage?: Usage
  title?: string
  contextChanges: { context: string; enabled: boolean }[]
}

export const emptyConversation: ConversationState = { lastSeq: 0, items: [], contextChanges: [] }

export function conversationReducer(state: ConversationState, event: EventEnvelope): ConversationState {
  if (event.seq <= state.lastSeq) {
    return state
  }

  const next: ConversationState = { ...state, lastSeq: event.seq }
  const payload = event.payload

  switch (event.type) {
    case 'message.user':
      return { ...next, items: [...state.items, { kind: 'user', seq: event.seq, text: text(payload.text) }] }

    case 'turn.started':
      return { ...next, activeTurn: event.turnId ?? undefined, usage: undefined }

    case 'assistant.delta':
      return { ...next, items: appendDelta(state.items, event.seq, text(payload.text)) }

    case 'assistant.message':
      return { ...next, items: finishAssistant(state.items, event.seq, text(payload.text)) }

    case 'tool.search':
      return {
        ...next,
        items: [
          ...state.items,
          {
            kind: 'toolSearch',
            seq: event.seq,
            goal: text(payload.goal),
            strategy: text(payload.strategy),
            tools: (payload.tools as SelectedTool[] | undefined) ?? [],
            blocked: (payload.blocked as BlockedTool[] | undefined) ?? [],
            durationMs: Number(payload.durationMs ?? 0),
          },
        ],
      }

    case 'context.suggested':
      return { ...next, items: [...state.items, { kind: 'contextSuggestion', seq: event.seq, contexts: (payload.contexts as string[]) ?? [] }] }

    case 'tool.started':
      return {
        ...next,
        items: [
          ...state.items,
          {
            kind: 'toolCall',
            seq: event.seq,
            callId: text(payload.callId),
            tool: text(payload.tool),
            effect: text(payload.effect),
            status: 'running',
            arguments: payload.arguments,
          },
        ],
      }

    case 'tool.blocked':
      return {
        ...next,
        items: [
          ...state.items,
          {
            kind: 'toolCall',
            seq: event.seq,
            callId: text(payload.callId),
            tool: text(payload.tool),
            status: 'blocked',
            reason: text(payload.reason),
            missing: (payload.missing as string[]) ?? [],
          },
        ],
      }

    case 'tool.completed':
      return {
        ...next,
        items: updateLast(
          state.items,
          (item) => item.kind === 'toolCall' && item.callId === payload.callId && item.status === 'running',
          (item) => ({
            ...item,
            status: payload.ok ? 'ok' : 'error',
            summary: optional(payload.summary),
            error: optional(payload.error),
          }) as TimelineItem,
        ),
      }

    case 'approval.requested':
      return {
        ...next,
        items: [
          ...state.items,
          {
            kind: 'approval',
            seq: event.seq,
            approvalId: text(payload.approvalId),
            tool: text(payload.tool),
            status: 'pending',
            severity: payload.severity === 'danger' ? 'danger' : 'warning',
            description: text(payload.description),
            args: payload.arguments,
            expiresAt: text(payload.expiresAt),
          },
        ],
      }

    case 'approval.resolved':
      return {
        ...next,
        items: updateLast(
          state.items,
          (item) => item.kind === 'approval' && item.approvalId === payload.approvalId,
          (item) => ({ ...item, status: String(payload.status).toLowerCase() }) as TimelineItem,
        ),
      }

    case 'usage.updated':
      return { ...next, usage: payload as unknown as Usage }

    case 'turn.completed':
      return {
        ...next,
        activeTurn: undefined,
        usage: (payload.usage as Usage | undefined) ?? state.usage,
        items: [...stopStreaming(state.items), { kind: 'turnEnd', seq: event.seq, ok: true, durationMs: Number(payload.durationMs ?? 0) }],
      }

    case 'turn.failed':
      return {
        ...next,
        activeTurn: undefined,
        items: [
          ...stopStreaming(expirePending(state.items)),
          { kind: 'turnEnd', seq: event.seq, ok: false, code: text(payload.code), message: text(payload.message) },
        ],
      }

    case 'context.changed':
      return { ...next, contextChanges: [...state.contextChanges, { context: text(payload.context), enabled: Boolean(payload.enabled) }] }

    case 'conversation.renamed':
      return { ...next, title: text(payload.title) }

    default:
      return next
  }
}

export function pendingApproval(state: ConversationState): Extract<TimelineItem, { kind: 'approval' }> | undefined {
  return state.items.find((item): item is Extract<TimelineItem, { kind: 'approval' }> => item.kind === 'approval' && item.status === 'pending')
}

function appendDelta(items: TimelineItem[], seq: number, piece: string): TimelineItem[] {
  const last = items.at(-1)

  if (last?.kind === 'assistant' && last.streaming) {
    return [...items.slice(0, -1), { ...last, text: last.text + piece }]
  }

  return [...items, { kind: 'assistant', seq, text: piece, streaming: true }]
}

/**
 * A resposta final substitui o texto que veio em pedaços (é a mesma coisa, inteira).
 */
function finishAssistant(items: TimelineItem[], seq: number, final: string): TimelineItem[] {
  const last = items.at(-1)

  if (last?.kind === 'assistant' && last.streaming) {
    return [...items.slice(0, -1), { ...last, text: final, streaming: false }]
  }

  return [...items, { kind: 'assistant', seq, text: final, streaming: false }]
}

function stopStreaming(items: TimelineItem[]): TimelineItem[] {
  return items.map((item) => (item.kind === 'assistant' && item.streaming ? { ...item, streaming: false } : item))
}

/**
 * Turno falhou com aprovação ainda aberta: o cartão não pode continuar com botões.
 */
function expirePending(items: TimelineItem[]): TimelineItem[] {
  return items.map((item) => (item.kind === 'approval' && item.status === 'pending' ? { ...item, status: 'rejected' } : item))
}

function updateLast(items: TimelineItem[], matches: (item: TimelineItem) => boolean, change: (item: TimelineItem) => TimelineItem): TimelineItem[] {
  for (let index = items.length - 1; index >= 0; index--) {
    const item = items[index]!

    if (matches(item)) {
      return [...items.slice(0, index), change(item), ...items.slice(index + 1)]
    }
  }

  return items
}

function text(value: unknown): string {
  return typeof value === 'string' ? value : value == null ? '' : String(value)
}

function optional(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined
}
