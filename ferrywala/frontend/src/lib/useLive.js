import { useEffect, useRef } from 'react'
import { subscribe } from './realtime'

/** Calls `handler(message)` for every push on `topic`. No-op when topic is falsy. */
export function useLive(topic, handler) {
  const ref = useRef(handler)
  useEffect(() => {
    ref.current = handler
  })
  useEffect(() => {
    if (!topic) return undefined
    return subscribe(topic, (m) => ref.current(m))
  }, [topic])
}
