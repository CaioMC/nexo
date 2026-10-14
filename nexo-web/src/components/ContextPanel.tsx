import type { Context } from '../domain/contexts'
import { ContextCard } from './ContextCard'

export function ContextPanel({ contexts, busy, onToggle, onManage }: { contexts: Context[]; busy?: string; onToggle: (context: Context) => void; onManage: () => void }) {
  const enabled = contexts.filter((context) => context.enabled).length

  return (
    <aside className="context-panel" aria-label="Contextos">
      <header>
        <h2>Contextos</h2>
        <span className="muted small">
          {enabled} de {contexts.length} ligados
        </span>
      </header>
      <div className="context-list">
        {contexts.map((context) => (
          <ContextCard key={context.key} context={context} busy={busy === context.key} onToggle={onToggle} compact />
        ))}
      </div>
      <button type="button" className="link" onClick={onManage}>
        Gerenciar contextos
      </button>
    </aside>
  )
}
