import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { getPosition, reverseLabel } from '../lib/geo'

const KEY = 'fw_location'
const LocationContext = createContext(null)

function load() {
  try {
    const raw = JSON.parse(localStorage.getItem(KEY))
    if (raw && Number.isFinite(raw.lat) && Number.isFinite(raw.lng)) return raw
  } catch { /* ignore */ }
  return null
}

/**
 * The customer's chosen meeting point. Set once (GPS or manual) and remembered,
 * so we never track continuously and never silently substitute a fake city.
 */
export function LocationProvider({ children }) {
  const [location, setLocationState] = useState(load)
  const [status, setStatus] = useState('idle') // idle | locating | denied | error
  const [message, setMessage] = useState('')

  const set = useCallback((loc) => {
    setLocationState(loc)
    try { localStorage.setItem(KEY, JSON.stringify(loc)) } catch { /* private mode */ }
  }, [])

  const useMyLocation = useCallback(async () => {
    setStatus('locating')
    setMessage('')
    try {
      const { lat, lng } = await getPosition()
      const label = (await reverseLabel(lat, lng)) || 'Your location'
      set({ lat, lng, label, source: 'gps' })
      setStatus('idle')
      return true
    } catch (e) {
      setStatus(e.code === 'DENIED' ? 'denied' : 'error')
      setMessage(e.code)
      return false
    }
  }, [set])

  const value = useMemo(() => ({
    location, status, message, setLocation: set, useMyLocation,
    clear: () => { setLocationState(null); try { localStorage.removeItem(KEY) } catch { /* ignore */ } },
  }), [location, status, message, set, useMyLocation])

  return <LocationContext.Provider value={value}>{children}</LocationContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useLocation2() {
  return useContext(LocationContext)
}
