const rupee = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 })

export const money = (n) => `₹${rupee.format(Number(n ?? 0))}`

export function timeAgo(iso) {
  if (!iso) return ''
  const s = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000)
  if (s < 45) return 'just now'
  if (s < 3600) return `${Math.round(s / 60)} min ago`
  if (s < 86400) return `${Math.round(s / 3600)} h ago`
  return `${Math.round(s / 86400)} d ago`
}

export const km = (d) => (d == null ? '' : d < 1 ? `${Math.round(d * 1000)} m` : `${d.toFixed(1)} km`)

export const initials = (name = '') =>
  name.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('') || 'F'

export function estimateRange(min, max) {
  if (max == null || max <= min) return `${min}`
  return `${min}–${max}`
}

export function navigateUrl(lat, lng) {
  return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`
}
