import { useCallback, useSyncExternalStore } from 'react'

const KEY = 'fw_favs'
const listeners = new Set()
const read = () => { try { return JSON.parse(localStorage.getItem(KEY)) || [] } catch { return [] } }
let cache = read()
const emit = () => listeners.forEach((l) => l())

/** Saved vendors, kept on this device only (localStorage). Not synced to the account. */
export function useFavourites() {
  const ids = useSyncExternalStore((cb) => { listeners.add(cb); return () => listeners.delete(cb) }, () => cache, () => [])
  const toggle = useCallback((id) => {
    cache = cache.includes(id) ? cache.filter((x) => x !== id) : [...cache, id].slice(-100)
    try { localStorage.setItem(KEY, JSON.stringify(cache)) } catch { /* private mode */ }
    emit()
  }, [])
  return { ids, has: (id) => ids.includes(id), toggle }
}
