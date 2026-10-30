// One shared STOMP connection, loaded lazily (SockJS + STOMP only download
// when a screen actually needs live updates). Reconnects automatically and
// re-subscribes. The server only lets you read: public vendor status, and
// your own /user/queue/* destinations.
import { tokenStore } from './api'

const WS_URL = import.meta.env.VITE_WS_URL ?? `${window.location.origin}/ws`

let client = null
let connecting = null
const subs = new Map() // topic -> { handlers:Set, sub }
let users = 0

async function ensureClient() {
  if (client) return client
  if (connecting) return connecting
  connecting = (async () => {
    const [{ Client }, sockjs] = await Promise.all([import('@stomp/stompjs'), import('sockjs-client')])
    const SockJS = sockjs.default ?? sockjs
    const c = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: tokenStore.get() ? { Authorization: `Bearer ${tokenStore.get()}` } : {},
      beforeConnect: () => {
        // token may have changed since the client was created
        const t = tokenStore.get()
        c.connectHeaders = t ? { Authorization: `Bearer ${t}` } : {}
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 15000,
      heartbeatOutgoing: 15000,
      onConnect: () => {
        subs.forEach((entry, topic) => {
          entry.sub = c.subscribe(topic, (msg) => {
            let data
            try { data = JSON.parse(msg.body) } catch { return }
            entry.handlers.forEach((h) => h(data))
          })
        })
      },
    })
    c.activate()
    client = c
    return c
  })()
  try {
    return await connecting
  } finally {
    connecting = null
  }
}

/** Subscribe to a topic. Returns an unsubscribe function. */
export function subscribe(topic, handler) {
  let entry = subs.get(topic)
  if (!entry) {
    entry = { handlers: new Set(), sub: null }
    subs.set(topic, entry)
  }
  entry.handlers.add(handler)
  users += 1
  let cancelled = false

  ensureClient().then((c) => {
    if (cancelled) return
    const e = subs.get(topic)
    if (c.connected && e && !e.sub) {
      e.sub = c.subscribe(topic, (msg) => {
        let data
        try { data = JSON.parse(msg.body) } catch { return }
        e.handlers.forEach((h) => h(data))
      })
    }
  }).catch(() => { /* realtime is a bonus; screens keep working without it */ })

  return () => {
    cancelled = true
    const e = subs.get(topic)
    if (e) {
      e.handlers.delete(handler)
      if (e.handlers.size === 0) {
        try { e.sub?.unsubscribe() } catch { /* already closed */ }
        subs.delete(topic)
      }
    }
    users -= 1
    if (users <= 0) disconnect()
  }
}

export function disconnect() {
  users = 0
  subs.clear()
  if (client) {
    client.deactivate()
    client = null
  }
}
