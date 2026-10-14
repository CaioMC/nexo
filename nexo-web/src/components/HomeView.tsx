import { Composer } from './Composer'

const SUGGESTIONS = ['O que tenho amanhã na agenda?', 'Resuma os e-mails não lidos de hoje', 'Liste os arquivos que mudei esta semana no Drive', 'Crie um doc com a pauta da reunião de segunda']

export function HomeView({ enabledContexts, onStart, onContexts }: { enabledContexts: number; onStart: (text: string) => void; onContexts: () => void }) {
  return (
    <section className="chat home">
      <div className="empty">
        <h1>Como posso ajudar?</h1>
        {enabledContexts === 0 ? (
          <p className="alert alert-info">
            Nenhum contexto ligado ainda. <button type="button" className="link" onClick={onContexts}>Escolha o que o assistente pode usar</button>.
          </p>
        ) : (
          <p className="muted">{enabledContexts} contexto(s) ligado(s).</p>
        )}
        <div className="suggestions">
          {SUGGESTIONS.map((suggestion) => (
            <button key={suggestion} type="button" className="suggestion-chip" onClick={() => onStart(suggestion)}>
              {suggestion}
            </button>
          ))}
        </div>
      </div>
      <Composer busy={false} onSend={onStart} onStop={() => undefined} />
    </section>
  )
}
