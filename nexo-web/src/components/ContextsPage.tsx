import type { Context } from '../domain/contexts'
import { ContextCard } from './ContextCard'

export function ContextsPage({ contexts, busy, error, onToggle }: { contexts: Context[]; busy?: string; error?: string; onToggle: (context: Context) => void }) {
  return (
    <section className="page">
      <h1>Contextos</h1>
      <p className="muted">
        Escolha o que o assistente pode usar. Desligar vale na hora, inclusive no meio de uma conversa. Para revogar tudo no Google, use
        “Desconectar conta” em Conta.
      </p>
      {error && <p className="alert alert-danger">{error}</p>}
      <div className="context-grid">
        {contexts.map((context) => (
          <ContextCard key={context.key} context={context} busy={busy === context.key} onToggle={onToggle} />
        ))}
      </div>
    </section>
  )
}
