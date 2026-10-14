import { contextName, type Context } from '../domain/contexts'

export function ContextSuggestion({ contexts, all, onEnable }: { contexts: string[]; all: Context[]; onEnable: (key: string) => void }) {
  return (
    <div className="card suggestion">
      <span>Para isso preciso de {contexts.map((key) => contextName(key, all)).join(' e ')}.</span>
      <div className="row">
        {contexts
          .filter((key) => !all.find((context) => context.key === key)?.enabled)
          .map((key) => (
            <button key={key} type="button" className="button button-primary" onClick={() => onEnable(key)}>
              Ligar {contextName(key, all)}
            </button>
          ))}
      </div>
    </div>
  )
}
