import type { TimelineItem } from '../domain/conversationReducer'
import { toolLabel } from '../domain/tools'

const ICONS = { running: '⏳', ok: '✔', error: '✖', blocked: '⛔' } as const

export function ToolCallCard({ item }: { item: Extract<TimelineItem, { kind: 'toolCall' }> }) {
  return (
    <div className={`tool-call tool-call-${item.status}`}>
      <span className="tool-icon" aria-hidden="true">
        {ICONS[item.status]}
      </span>
      <code>{toolLabel(item.tool)}</code>
      <span className="muted">{describe(item)}</span>
    </div>
  )
}

function describe(item: Extract<TimelineItem, { kind: 'toolCall' }>): string {
  switch (item.status) {
    case 'running':
      return 'executando…'
    case 'ok':
      return item.summary ?? 'ok'
    case 'error':
      return item.error ?? 'falhou'
    case 'blocked':
      return item.reason === 'CONTEXT_DISABLED' ? `contexto desligado: ${item.missing?.join(', ')}` : `falta executar antes: ${item.missing?.map(toolLabel).join(', ')}`
  }
}
