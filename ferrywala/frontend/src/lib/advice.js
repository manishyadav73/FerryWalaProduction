// Turns the demand engine's structured result into plain sentences in the user's language.
// The server still sends English "reasons"; we rebuild them here from the numbers so the
// explanation is readable in Hindi/Tamil too, and never says more than the data supports.

const nameList = (names, pname) => names.map((n) => pname(n)).join(', ')

export function buildReasons(o, { t, pname, hour, dist }) {
  const out = []
  out.push(t('why.interest', { n: o.customerCount, s: o.signalCount }))
  if (o.topProducts.length) out.push(t('why.top', { list: nameList(o.topProducts.map((p) => p.name), pname) }))
  const w = `${hour(o.recommendedWindow.startHour)} – ${hour(o.recommendedWindow.endHour)}`
  out.push(t(o.recommendedWindow.fromData ? 'why.timeData' : 'why.timeTypical', { w }))
  if (o.distanceKm != null) out.push(t('why.distance', { d: dist(o.distanceKm) }))
  const missing = o.topProducts.filter((p) => !p.vendorCarries).map((p) => p.name)
  if (missing.length) out.push(t('why.notCarried', { list: nameList(missing, pname) }))
  out.push(t(`why.conf${o.confidence}`))
  return out
}

/** One short paragraph, suitable for reading aloud. */
export function buildSummary(o, { t, pname, hour, dist }) {
  const parts = [
    t('say.go', { area: o.areaName }),
    t('say.customers', { min: o.estimatedCustomersMin, max: o.estimatedCustomersMax }),
  ]
  if (o.topProducts.length) parts.push(t('say.products', { list: nameList(o.topProducts.slice(0, 3).map((p) => p.name), pname) }))
  parts.push(t('say.time', { w: `${hour(o.recommendedWindow.startHour)} – ${hour(o.recommendedWindow.endHour)}` }))
  if (o.distanceKm != null) parts.push(t('say.distance', { d: dist(o.distanceKm) }))
  parts.push(t('say.estimate'))
  return parts.join(' ')
}
