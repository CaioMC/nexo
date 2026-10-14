import type { Conversation } from '../api/conversationsApi'
import type { Me } from '../api/meApi'
import { initial } from './AccountPage'

/**
 * Conversas, mais recentes primeiro, agrupadas por dia.
 */
export function ConversationSidebar({
  me,
  conversations,
  activeId,
  onNew,
  onOpen,
  onDelete,
  onSettings,
  onContexts,
}: {
  me: Me
  conversations: Conversation[]
  activeId?: string
  onNew: () => void
  onOpen: (id: string) => void
  onDelete: (id: string) => void
  onSettings: () => void
  onContexts: () => void
}) {
  return (
    <nav className="sidebar" aria-label="Conversas">
      <button type="button" className="button button-primary new" onClick={onNew}>
        + Nova conversa
      </button>
      <div className="conversation-list">
        {groups(conversations).map(([label, items]) => (
          <div key={label}>
            <h2 className="group">{label}</h2>
            <ul>
              {items.map((conversation) => (
                <li key={conversation.id} className={conversation.id === activeId ? 'active' : ''}>
                  <button type="button" className="conversation" onClick={() => onOpen(conversation.id)} title={conversation.title}>
                    {conversation.activeTurn && <span className="live-dot" aria-label="turno em andamento" />}
                    {conversation.title}
                  </button>
                  <button type="button" className="icon-button" aria-label={`Apagar ${conversation.title}`} onClick={() => onDelete(conversation.id)}>
                    ×
                  </button>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <footer className="sidebar-footer">
        <button type="button" className="me" onClick={onSettings}>
          {me.pictureUrl ? <img src={me.pictureUrl} alt="" className="avatar" referrerPolicy="no-referrer" /> : <span className="avatar">{initial(me)}</span>}
          <span className="me-name">{me.displayName || me.email}</span>
        </button>
        <button type="button" className="icon-button" aria-label="Contextos" title="Contextos" onClick={onContexts}>
          ⚙
        </button>
      </footer>
    </nav>
  )
}

function groups(conversations: Conversation[]): [string, Conversation[]][] {
  const today = new Date()
  const yesterday = new Date(today.getTime() - 86_400_000)
  const map = new Map<string, Conversation[]>()

  for (const conversation of conversations) {
    const day = new Date(conversation.updatedAt)
    const label = same(day, today) ? 'Hoje' : same(day, yesterday) ? 'Ontem' : 'Anteriores'

    map.set(label, [...(map.get(label) ?? []), conversation])
  }

  return [...map.entries()]
}

function same(first: Date, second: Date): boolean {
  return first.toDateString() === second.toDateString()
}
