import { useEffect, useRef, useState, useSyncExternalStore } from 'react'

const QUERY = '(prefers-reduced-motion: reduce)'

/** True when the user asked the OS for less motion. Updates live. */
export function useReducedMotion() {
  return useSyncExternalStore(
    (cb) => {
      const m = window.matchMedia?.(QUERY)
      m?.addEventListener?.('change', cb)
      return () => m?.removeEventListener?.('change', cb)
    },
    () => !!window.matchMedia?.(QUERY).matches,
    () => false,
  )
}

/**
 * Returns [ref, shown]. `shown` flips to true once the element scrolls into view (once).
 * Without IntersectionObserver, or with reduced motion, it is true immediately so content is never hidden.
 */
export function useInView({ margin = '0px 0px -8% 0px' } = {}) {
  const ref = useRef(null)
  const reduce = useReducedMotion()
  const supported = typeof IntersectionObserver !== 'undefined'
  const [seen, setSeen] = useState(false)
  useEffect(() => {
    const el = ref.current
    if (!el || reduce || !supported) return undefined
    const io = new IntersectionObserver((entries) => {
      if (entries.some((e) => e.isIntersecting)) { setSeen(true); io.disconnect() }
    }, { rootMargin: margin, threshold: 0.12 })
    io.observe(el)
    return () => io.disconnect()
  }, [reduce, supported, margin])
  return [ref, reduce || !supported || seen]
}

/** Online/offline from the browser. Only says what the browser knows; it cannot prove the API is reachable. */
export function useOnline() {
  return useSyncExternalStore(
    (cb) => {
      window.addEventListener('online', cb)
      window.addEventListener('offline', cb)
      return () => { window.removeEventListener('online', cb); window.removeEventListener('offline', cb) }
    },
    () => navigator.onLine !== false,
    () => true,
  )
}
