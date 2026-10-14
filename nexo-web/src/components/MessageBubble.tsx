import { Fragment, type ReactNode } from 'react'

/**
 * Texto do assistente com o mínimo de Markdown que o modelo usa: **negrito**, `código` e quebras de linha.
 */
export function MessageBubble({ role, text, streaming = false }: { role: 'user' | 'assistant'; text: string; streaming?: boolean }) {
  return (
    <div className={`bubble bubble-${role}`}>
      {role === 'assistant' ? format(text) : text}
      {streaming && <span className="caret" aria-hidden="true" />}
    </div>
  )
}

function format(text: string): ReactNode {
  return text.split('\n').map((line, index) => (
    <Fragment key={index}>
      {index > 0 && <br />}
      {inline(line)}
    </Fragment>
  ))
}

function inline(line: string): ReactNode[] {
  return line.split(/(\*\*[^*]+\*\*|`[^`]+`)/g).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return <strong key={index}>{part.slice(2, -2)}</strong>
    }

    if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      return <code key={index}>{part.slice(1, -1)}</code>
    }

    return <Fragment key={index}>{part}</Fragment>
  })
}
