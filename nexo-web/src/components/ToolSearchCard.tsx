import type { TimelineItem } from '../domain/conversationReducer'
import { toolLabel } from '../domain/tools'

/**
 * O que o RAG achou e o que o DAG acrescentou (RF-24), na ordem de execução.
 */
export function ToolSearchCard({ item }: { item: Extract<TimelineItem, { kind: 'toolSearch' }> }) {
  return (
    <details className="card tool-search">
      <summary>
        <span aria-hidden="true">🔎</span> Buscou ferramentas: <span className="muted">{item.goal}</span>
        <span className="chips">
          {item.tools.map((tool) => (
            <span key={tool.name} className={`chip chip-${tool.origin.toLowerCase()}`} title={tool.origin === 'DAG' ? 'Pré-requisito trazido pelo DAG' : 'Encontrada pelo RAG'}>
              {toolLabel(tool.name)}
              {tool.origin === 'DAG' && <em> DAG</em>}
            </span>
          ))}
        </span>
      </summary>
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Ferramenta</th>
            <th>Origem</th>
            <th>Nível</th>
            <th>Score</th>
          </tr>
        </thead>
        <tbody>
          {item.tools.map((tool) => (
            <tr key={tool.name}>
              <td>{tool.step}</td>
              <td>
                <code>{tool.name}</code>
              </td>
              <td>{tool.origin}</td>
              <td>{tool.level ?? '–'}</td>
              <td>{tool.score.toFixed(4)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="muted small">
        {item.strategy} · {item.durationMs} ms{item.blocked.length > 0 && ` · ${item.blocked.length} bloqueada(s) por contexto desligado`}
      </p>
    </details>
  )
}
