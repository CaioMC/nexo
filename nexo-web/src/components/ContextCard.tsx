import { useState } from 'react'

import { look, shortScope, type Context } from '../domain/contexts'

/**
 * Um contexto com interruptor (doc 12): ligar sem escopo concedido leva ao Google.
 */
export function ContextCard({ context, busy, onToggle, compact = false }: { context: Context; busy?: boolean; onToggle: (context: Context) => void; compact?: boolean }) {
  const [open, setOpen] = useState(false)
  const { icon, color } = look(context.key)
  const label = context.enabled ? `Desligar ${context.name}` : `Ligar ${context.name}`

  return (
    <article className={`context-card ${compact ? 'compact' : ''} ${context.enabled ? 'on' : ''}`}>
      <span className="context-icon" style={{ color }} aria-hidden="true">
        {icon}
      </span>
      <div className="context-body">
        <div className="context-head">
          <h3>{context.name}</h3>
          {!compact && <span className="muted small">{context.toolCount} ferramentas</span>}
        </div>
        {!compact && <p className="muted">{context.description}</p>}
        {!compact && !context.enabled && !context.scopesGranted && <p className="hint">Ao ligar, o Google vai pedir sua permissão.</p>}
        {!compact && (
          <button type="button" className="link" aria-expanded={open} onClick={() => setOpen(!open)}>
            {open ? 'Esconder permissões' : 'Ver permissões'}
          </button>
        )}
        {open && (
          <ul className="scopes">
            {context.scopes.map((scope) => (
              <li key={scope}>
                <code>{shortScope(scope)}</code>
              </li>
            ))}
          </ul>
        )}
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={context.enabled}
        aria-label={label}
        title={label}
        className="switch"
        disabled={busy}
        onClick={() => onToggle(context)}
      >
        <span className="knob" />
      </button>
    </article>
  )
}
