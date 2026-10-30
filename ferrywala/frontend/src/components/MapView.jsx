import { useEffect } from 'react'
import { Circle, MapContainer, Marker, Polyline, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { DEFAULT_CENTER } from '../lib/geo'

// No marker image files: every marker is a small inline-styled div, so there is
// nothing to hot-link or fail to load on slow networks.
function icon(kind, { online = true, label = '' } = {}) {
  const cls = `pin pin-${kind} ${online ? '' : 'pin-off'}`
  const inner = kind === 'me' ? '<i></i>' : `<span>${label}</span>`
  return L.divIcon({ className: 'pin-wrap', html: `<div class="${cls}">${inner}</div>`, iconSize: [34, 34], iconAnchor: [17, 34], popupAnchor: [0, -30] })
}

function Fit({ points, fallback }) {
  const map = useMap()
  useEffect(() => {
    if (points.length > 1) map.fitBounds(L.latLngBounds(points), { padding: [36, 36], maxZoom: 15 })
    else if (points.length === 1) map.setView(points[0], 15)
    else map.setView(fallback, 11)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [map, JSON.stringify(points)])
  return null
}

function Picker({ onPick }) {
  useMapEvents({ click: (e) => onPick?.({ lat: e.latlng.lat, lng: e.latlng.lng }) })
  return null
}

/** lines: [{ id, points:[[lat,lng],…], color }] (straight lines, never routes)
 * markers: [{ id, lat, lng, kind:'vendor'|'me'|'demand', online, label, title, body, onClick }] */
export default function MapView({ markers = [], circles = [], lines = [], onPick, height = 320, label = 'Map' }) {
  const points = markers.filter((m) => Number.isFinite(m.lat) && Number.isFinite(m.lng)).map((m) => [m.lat, m.lng])
  return (
    <div className="map-box" style={{ height }} role="region" aria-label={label}>
      <MapContainer center={points[0] ?? DEFAULT_CENTER} zoom={13} scrollWheelZoom={false} style={{ height: '100%', width: '100%' }}>
        <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" maxZoom={19} />
        <Fit points={points} fallback={DEFAULT_CENTER} />
        {onPick && <Picker onPick={onPick} />}
        {circles.map((c) => (
          <Circle key={c.id} center={[c.lat, c.lng]} radius={c.radius} pathOptions={{ color: c.color, fillColor: c.color, fillOpacity: 0.22, weight: 2 }} />
        ))}
        {lines.map((l) => (
          <Polyline key={l.id} positions={l.points} pathOptions={{ color: l.color || '#e8590c', weight: 3, dashArray: '2 8', className: 'route-line' }} />
        ))}
        {markers.map((m) => (
          <Marker
            key={m.id}
            position={[m.lat, m.lng]}
            icon={icon(m.kind, { online: m.online, label: m.label })}
            title={m.title || m.label || undefined}
            alt={m.title || m.label || "Map marker"}
            eventHandlers={m.onClick ? { click: m.onClick } : undefined}
          >
            {(m.title || m.body) && <Popup><b>{m.title}</b>{m.body && <div>{m.body}</div>}</Popup>}
          </Marker>
        ))}
      </MapContainer>
    </div>
  )
}
