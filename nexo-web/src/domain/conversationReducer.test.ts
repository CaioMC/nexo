import { conversationReducer, emptyConversation, pendingApproval, type ConversationState } from './conversationReducer'
import type { EventEnvelope } from './events'
import { approvedTurn, event, resetSeq } from './fixtures'

function apply(events: EventEnvelope[], state: ConversationState = emptyConversation): ConversationState {
  return events.reduce(conversationReducer, state)
}

describe('conversationReducer', () => {
  beforeEach(() => resetSeq())

  it('monta a timeline de um turno inteiro', () => {
    const state = apply(approvedTurn())

    expect(state.items.map((item) => item.kind)).toEqual(['user', 'toolSearch', 'toolCall', 'approval', 'toolCall', 'assistant', 'turnEnd'])
    expect(state.activeTurn).toBeUndefined()
    expect(state.lastSeq).toBe(14)
    expect(state.usage?.modelCalls).toBe(4)
  })

  it('pedaços de texto viram uma bolha só, trocada pela resposta final', () => {
    const state = apply(approvedTurn().slice(0, 11))
    const streaming = state.items.at(-1)

    expect(streaming).toMatchObject({ kind: 'assistant', text: 'Pronto, reunião criada.', streaming: true })
    expect(apply(approvedTurn()).items.find((item) => item.kind === 'assistant')).toMatchObject({ streaming: false })
  })

  it('ignora eventos já aplicados (replay depois de reconectar)', () => {
    const events = approvedTurn()
    const once = apply(events)

    expect(apply(events, once)).toBe(once)
    expect(apply([...events.slice(0, 6), ...events.slice(3)]).items).toHaveLength(once.items.length)
  })

  it('a chamada termina no mesmo cartão em que começou', () => {
    const calls = apply(approvedTurn()).items.filter((item) => item.kind === 'toolCall')

    expect(calls).toEqual([
      expect.objectContaining({ callId: 'c1', status: 'ok', summary: '1 itens' }),
      expect.objectContaining({ callId: 'c3', status: 'ok' }),
    ])
  })

  it('aprovação pendente some quando é resolvida', () => {
    const events = approvedTurn()

    expect(pendingApproval(apply(events.slice(0, 6)))?.approvalId).toBe('a-1')
    expect(pendingApproval(apply(events.slice(0, 7)))).toBeUndefined()
  })

  it('bloqueio, sugestão de contexto e falha do turno', () => {
    const state = apply([
      event('message.user', { text: 'leia meus e-mails' }),
      event('turn.started', {}),
      event('context.suggested', { contexts: ['gmail'], tools: ['google_gmail_search'] }),
      event('tool.blocked', { callId: 'b1', tool: 'google_docs_append_text', reason: 'MISSING_PREREQUISITES', missing: ['google_docs_read'] }),
      event('approval.requested', { approvalId: 'a-9', tool: 'x', severity: 'danger', description: 'Apagar', arguments: {} }),
      event('turn.failed', { code: 'STOPPED', message: 'parado' }),
      event('context.changed', { context: 'gmail', enabled: true }, null),
      event('conversation.renamed', { title: 'E-mails' }, null),
    ])

    expect(state.items).toContainEqual(expect.objectContaining({ kind: 'contextSuggestion', contexts: ['gmail'] }))
    expect(state.items).toContainEqual(expect.objectContaining({ kind: 'toolCall', status: 'blocked', missing: ['google_docs_read'] }))
    expect(state.items).toContainEqual(expect.objectContaining({ kind: 'approval', status: 'rejected', severity: 'danger' }))
    expect(state.items.at(-1)).toMatchObject({ kind: 'turnEnd', ok: false, code: 'STOPPED' })
    expect(state.activeTurn).toBeUndefined()
    expect(state.contextChanges).toEqual([{ context: 'gmail', enabled: true }])
    expect(state.title).toBe('E-mails')
  })
})
