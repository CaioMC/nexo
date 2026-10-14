import { useState, type FormEvent, type KeyboardEvent } from 'react'

/**
 * Campo de mensagem. Com turno ativo, vira o botão Parar (RF-23).
 */
export function Composer({ busy, disabled, onSend, onStop }: { busy: boolean; disabled?: boolean; onSend: (text: string) => void; onStop: () => void }) {
  const [text, setText] = useState('')

  function submit(event?: FormEvent) {
    event?.preventDefault()

    const message = text.trim()

    if (!message || busy || disabled) {
      return
    }

    onSend(message)
    setText('')
  }

  function onKey(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      submit()
      event.preventDefault()
    }
  }

  return (
    <form className="composer" onSubmit={submit}>
      <textarea
        aria-label="Mensagem"
        placeholder={busy ? 'Aguardando o assistente…' : 'Escreva… (Enter envia, Shift+Enter quebra linha)'}
        value={text}
        maxLength={8000}
        rows={1}
        disabled={busy || disabled}
        onChange={(event) => setText(event.target.value)}
        onKeyDown={onKey}
      />
      {busy ? (
        <button type="button" className="button button-danger-outline" onClick={onStop}>
          Parar
        </button>
      ) : (
        <button type="submit" className="button button-primary" disabled={!text.trim() || disabled} aria-label="Enviar">
          ➤
        </button>
      )}
    </form>
  )
}
