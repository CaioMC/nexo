import { approvedTurn } from '../domain/fixtures'
import type { EventEnvelope } from '../domain/events'
import { CommandRejected, ConversationSocket, type ConnectionState } from './conversationSocket'

/** WebSocket falso: o teste decide quando abre, o que chega e quando cai. */
class FakeSocket {
  static created: FakeSocket[] = []

  readyState = 0
  onopen?: () => void
  onmessage?: (message: { data: string }) => void
  onclose?: (event: { code: number }) => void
  sent: string[] = []

  constructor(readonly url: string) {
    FakeSocket.created.push(this)
  }

  open() {
    this.readyState = 1
    this.onopen?.()
  }

  send(data: string) {
    this.sent.push(data)
  }

  close() {
    this.onclose?.({ code: 1000 })
  }

  deliver(...messages: unknown[]) {
    messages.forEach((message) => this.onmessage?.({ data: JSON.stringify(message) }))
  }

  drop(code = 1006) {
    this.readyState = 3
    this.onclose?.({ code })
  }
}

describe('ConversationSocket', () => {
  beforeEach(() => {
    FakeSocket.created = []
    vi.useFakeTimers()
  })

  afterEach(() => vi.useRealTimers())

  function open(onEvent: (event: EventEnvelope) => void = () => {}, states: ConnectionState[] = []) {
    const socket = new ConversationSocket('c-1', 0, {
      onEvent,
      onState: (state) => states.push(state),
      create: (url) => new FakeSocket(url) as unknown as WebSocket,
      random: () => 1,
      baseUrl: 'ws://localhost:5173',
      newId: () => 'cmd-1',
    })

    socket.connect()
    FakeSocket.created.at(-1)!.open()

    return socket
  }

  it('conecta pedindo tudo desde o seq 0', () => {
    open()

    expect(FakeSocket.created[0]!.url).toBe('ws://localhost:5173/ws/conversations/c-1?lastSeq=0')
  })

  it('ao cair, reconecta com o maior seq recebido, com backoff, e não repete eventos', () => {
    const events = approvedTurn()
    const received: number[] = []
    const states: ConnectionState[] = []

    open((event) => received.push(event.seq), states)
    FakeSocket.created[0]!.deliver(...events.slice(0, 5))
    FakeSocket.created[0]!.drop()

    vi.advanceTimersByTime(499)
    expect(FakeSocket.created).toHaveLength(1)
    vi.advanceTimersByTime(1)

    const second = FakeSocket.created[1]!

    second.open()
    second.deliver(...events.slice(3))

    expect(second.url).toContain('lastSeq=5')
    expect(received).toEqual(events.map((event) => event.seq))
    expect(states).toEqual(['connecting', 'open', 'reconnecting', 'reconnecting', 'open'])
  })

  it('comando resolve no ack e rejeita no nack', async () => {
    const socket = open()
    const fake = FakeSocket.created[0]!
    const accepted = socket.send('user.message', { text: 'oi' })

    expect(JSON.parse(fake.sent[0]!)).toEqual({ kind: 'command', id: 'cmd-1', type: 'user.message', payload: { text: 'oi' } })
    fake.deliver({ kind: 'ack', commandId: 'cmd-1', turnId: 't-9' })
    await expect(accepted).resolves.toMatchObject({ turnId: 't-9' })

    const refused = socket.send('user.message', { text: 'de novo' })

    fake.deliver({ kind: 'nack', commandId: 'cmd-1', code: 'TURN_IN_PROGRESS', message: 'Já existe um turno em andamento.' })
    await expect(refused).rejects.toBeInstanceOf(CommandRejected)
  })

  it('conversa apagada ou de outro usuário não reconecta', () => {
    const states: ConnectionState[] = []

    open(() => {}, states)
    FakeSocket.created[0]!.drop(4404)
    vi.advanceTimersByTime(10_000)

    expect(FakeSocket.created).toHaveLength(1)
    expect(states.at(-1)).toBe('gone')
  })
})
