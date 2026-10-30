export const DEFAULT_CENTER = [13.0827, 80.2707] // only used to centre an empty map, never as "your" location

export function getPosition({ timeout = 12000 } = {}) {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(Object.assign(new Error('UNSUPPORTED'), { code: 'UNSUPPORTED' }))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (p) => resolve({ lat: p.coords.latitude, lng: p.coords.longitude, accuracy: p.coords.accuracy }),
      (err) => {
        const denied = err.code === 1
        reject(Object.assign(new Error(denied ? 'DENIED' : 'FAILED'), { code: denied ? 'DENIED' : 'FAILED' }))
      },
      { enableHighAccuracy: false, timeout, maximumAge: 60000 },
    )
  })
}

// Optional lookups via OpenStreetMap Nominatim (no API key; light, user-triggered use only).
export async function searchPlace(query, signal) {
  const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&countrycodes=in&q=${encodeURIComponent(query)}`
  const res = await fetch(url, { signal, headers: { Accept: 'application/json' } })
  if (!res.ok) throw Object.assign(new Error('SEARCH'), { code: 'SEARCH' })
  const rows = await res.json()
  return rows.map((r) => ({ lat: Number(r.lat), lng: Number(r.lon), label: r.display_name.split(',').slice(0, 2).join(',').trim() }))
}

export async function reverseLabel(lat, lng, signal) {
  try {
    const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=16&lat=${lat}&lon=${lng}`
    const res = await fetch(url, { signal, headers: { Accept: 'application/json' } })
    if (!res.ok) return null
    const r = await res.json()
    const a = r.address || {}
    return a.neighbourhood || a.suburb || a.residential || a.quarter || a.city_district || a.village || a.town || a.city || null
  } catch {
    return null
  }
}
