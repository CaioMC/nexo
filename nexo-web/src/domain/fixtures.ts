import type { EventEnvelope, EventType } from './events'

let seq = 0

export function resetSeq(): void {
  seq = 0
}

export function event(type: EventType, payload: Record<string, unknown> = {}, turnId: string | null = 't-1'): EventEnvelope {
  seq++

  return { kind: 'event', seq, conversationId: 'c-1', turnId, type, at: '2026-10-13T14:03:12.418Z', payload }
}

/**
 * Um turno completo com busca, duas leituras, aprovação e resposta em pedaços (doc 06, seção 5).
 */
export function approvedTurn(): EventEnvelope[] {
  resetSeq()

  return [
    event('message.user', { text: 'marque reunião amanhã 10h com a Ana', clientMessageId: 'm-1' }),
    event('turn.started', { model: 'gpt-5.4-mini', contexts: ['calendar'] }),
    event('tool.search', {
      goal: 'criar evento',
      strategy: 'HYBRID',
      durationMs: 12,
      tools: [
        { step: 1, name: 'google_calendar_list_calendars', origin: 'DAG', level: 0, score: 0 },
        { step: 2, name: 'google_calendar_list_events', origin: 'RAG', level: 1, score: 0.031 },
        { step: 3, name: 'google_calendar_create_event', origin: 'RAG', level: 2, score: 0.033 },
      ],
      blocked: [],
    }),
    event('tool.started', { callId: 'c1', tool: 'google_calendar_list_calendars', effect: 'READ', arguments: {} }),
    event('tool.completed', { callId: 'c1', tool: 'google_calendar_list_calendars', ok: true, summary: '1 itens', truncated: false }),
    event('approval.requested', {
      approvalId: 'a-1',
      callId: 'c3',
      tool: 'google_calendar_create_event',
      description: 'Criar o evento “Reunião com a Ana”',
      severity: 'warning',
      arguments: { summary: 'Reunião com a Ana' },
      expiresAt: '2026-10-13T14:18:12Z',
    }),
    event('approval.resolved', { approvalId: 'a-1', status: 'APPROVED' }),
    event('tool.started', { callId: 'c3', tool: 'google_calendar_create_event', effect: 'WRITE', arguments: {} }),
    event('tool.completed', { callId: 'c3', tool: 'google_calendar_create_event', ok: true, summary: 'Alteração feita', truncated: false }),
    event('assistant.delta', { text: 'Pronto, ' }),
    event('assistant.delta', { text: 'reunião criada.' }),
    event('usage.updated', { modelCalls: 4, inputTokens: 900, cachedInputTokens: 300, outputTokens: 80 }),
    event('assistant.message', { text: 'Pronto, reunião criada.' }),
    event('turn.completed', { durationMs: 4100, toolCalls: 2, usage: { modelCalls: 4, inputTokens: 900, cachedInputTokens: 300, outputTokens: 80 } }),
  ]
}
