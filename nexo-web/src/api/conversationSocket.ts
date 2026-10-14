import type { AckMessage, Command, CommandType, EventEnvelope, NackMessage, ServerMessage } from '../domain/events'

export type ConnectionState = 'connecting' | 'open' | 'reconnecting' | 'closed' | 'gone'

export interface ConversationSocketOptions {
  onEvent: (event: EventEnvelope) => void
  onState?: (state: ConnectionState) => void
  /** Fábrica do WebSocket (troca nos testes). */
  create?: (url: string) => WebSocket
  /** Espera mínima e máxima entre tentativas (backoff exponencial com jitter). */
  minDelay?: number
  maxDelay?: number
  random?: () => number
  baseUrl?: string
  newId?: () => string
}

export class CommandRejected extends Error {
  constructor(readonly code: string, message: string) {
    super(message)
  }
}

/** Fechamentos que não adianta tentar de novo (doc 06, seção 7). */
const FINAL_CLOSE = new Set([4400, 4401, 4404, 4409])

/**
 * O canal de uma conversa (doc 06). Guarda o maior `seq` recebido e, se a conexão cair, reconecta
 * com `lastSeq` (0,5 s → 8 s, com jitter): o servidor reenvia só o que faltou. Comandos devolvem
 * uma promessa resolvida no `ack` e rejeitada no `nack`.
 */
export class ConversationSocket {
  private socket?: WebSocket
  private lastSeq: number
  private attempt = 0
  private closedByUs = false
  private timer?: ReturnType<typeof setTimeout>
  private readonly pending = new Map<string, { resolve: (ack: AckMessage) => void; reject: (error: Error) => void }>()

  constructor(
    private readonly conversationId: string,
    lastSeq: number,
    private readonly options: ConversationSocketOptions,
  ) {
    this.lastSeq = lastSeq
  }

  connect(): void {
    this.closedByUs = false
    this.options.onState?.(this.attempt === 0 ? 'connecting' : 'reconnecting')

    const socket = (this.options.create ?? ((url) => new WebSocket(url)))(this.url())

    socket.onopen = () => {
      this.attempt = 0
      this.options.onState?.('open')
    }

    socket.onmessage = (message) => this.receive(JSON.parse(String(message.data)) as ServerMessage)

    socket.onclose = (close?: CloseEvent) => {
      this.failPending('Conexão perdida antes da resposta do servidor.')

      if (this.closedByUs) {
        this.options.onState?.('closed')

        return
      }

      if (close && FINAL_CLOSE.has(close.code)) {
        this.options.onState?.('gone')

        return
      }

      this.scheduleReconnect()
    }

    this.socket = socket
  }

  send(type: CommandType, payload: Record<string, unknown> = {}, id: string = this.newId()): Promise<AckMessage> {
    const command: Command = { kind: 'command', id, type, payload }

    if (!this.socket || this.socket.readyState !== 1) {
      return Promise.reject(new CommandRejected('NOT_CONNECTED', 'Sem conexão com o servidor. Tente de novo em instantes.'))
    }

    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject })
      this.socket!.send(JSON.stringify(command))
    })
  }

  close(): void {
    this.closedByUs = true
    clearTimeout(this.timer)
    this.socket?.close()
  }

  get seq(): number {
    return this.lastSeq
  }

  private receive(message: ServerMessage): void {
    if (message.kind === 'event') {
      if (message.seq > this.lastSeq) {
        this.lastSeq = message.seq
        this.options.onEvent(message)
      }

      return
    }

    if (message.kind === 'ack') {
      this.settle(message.commandId)?.resolve(message)

      return
    }

    this.reject(message)
  }

  private reject(nack: NackMessage): void {
    if (nack.commandId) {
      this.settle(nack.commandId)?.reject(new CommandRejected(nack.code, nack.message))
    }
  }

  private settle(id: string) {
    const entry = this.pending.get(id)

    this.pending.delete(id)

    return entry
  }

  private failPending(message: string): void {
    this.pending.forEach((entry) => entry.reject(new CommandRejected('DISCONNECTED', message)))
    this.pending.clear()
  }

  private scheduleReconnect(): void {
    const min = this.options.minDelay ?? 500
    const max = this.options.maxDelay ?? 8000
    const random = this.options.random ?? Math.random
    const base = Math.min(max, min * 2 ** this.attempt)
    const delay = Math.round(base / 2 + random() * (base / 2))

    this.attempt++
    this.options.onState?.('reconnecting')
    this.timer = setTimeout(() => this.connect(), delay)
  }

  private url(): string {
    const base = this.options.baseUrl ?? `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}`

    return `${base}/ws/conversations/${encodeURIComponent(this.conversationId)}?lastSeq=${this.lastSeq}`
  }

  private newId(): string {
    return (this.options.newId ?? (() => crypto.randomUUID()))()
  }
}
