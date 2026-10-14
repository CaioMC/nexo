import { useState } from 'react'

import type { TimelineItem } from '../domain/conversationReducer'
import { toolLabel } from '../domain/tools'

const STATUS: Record<string, string> = { pending: 'Aguardando', approved: 'Aprovado', rejected: 'Recusado', expired: 'Venceu sem resposta' }

/**
 * Pedido de confirmação de uma alteração (RF-36). A descrição é do servidor, não do modelo. Em
 * `danger`, aprovar pede um segundo clique.
 */
export function ApprovalCard({ item, onDecide }: { item: Extract<TimelineItem, { kind: 'approval' }>; onDecide: (approvalId: string, decision: 'approve' | 'reject') => void }) {
  const [armed, setArmed] = useState(false)
  const pending = item.status === 'pending'

  function approve() {
    if (item.severity === 'danger' && !armed) {
      setArmed(true)

      return
    }

    onDecide(item.approvalId, 'approve')
  }

  return (
    <section className={`card approval approval-${item.severity}`} aria-label="Confirmar alteração">
      <header>
        <strong>{item.severity === 'danger' ? 'Confirmar exclusão' : 'Confirmar alteração'}</strong>
        <code className="muted">{toolLabel(item.tool)}</code>
      </header>
      <p>{item.description}</p>
      <details>
        <summary>ver argumentos</summary>
        <pre>{JSON.stringify(item.args, null, 2)}</pre>
      </details>
      {pending ? (
        <div className="row end">
          <button type="button" className="button" onClick={() => onDecide(item.approvalId, 'reject')}>
            Recusar
          </button>
          <button type="button" className={`button ${item.severity === 'danger' ? 'button-danger' : 'button-primary'}`} onClick={approve}>
            {item.severity === 'danger' ? (armed ? 'Clique de novo para confirmar' : 'Aprovar exclusão') : 'Aprovar'}
          </button>
        </div>
      ) : (
        <p className={`status status-${item.status}`}>{STATUS[item.status]}</p>
      )}
    </section>
  )
}
