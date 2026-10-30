import { useEffect, useRef, useState } from 'react'
import { useLocation2 } from '../context/Location'
import { useI18n } from '../i18n'
import { reverseLabel, searchPlace } from '../lib/geo'
import Icon from './Icon'
import LazyMap from './LazyMap'
import { Spinner } from './States'

/**
 * Pick a place: GPS, search by area name, or tap the map. Permission denial
 * is a normal path, not an error screen: the other two options stay available.
 * `value`/`onChange` let callers (e.g. a vendor sharing their stall location)
 * reuse it without touching the customer's saved location.
 */
export default function LocationPicker({ value, onChange, autoLocate = false, showMap = true, title }) {
  const ctx = useLocation2()
  const { t } = useI18n()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [searching, setSearching] = useState(false)
  const [note, setNote] = useState('')
  const [mapOpen, setMapOpen] = useState(false)
  const [locating, setLocating] = useState(false)
  const tried = useRef(false)

  async function locate() {
    setNote('')
    setLocating(true)
    const { getPosition } = await import('../lib/geo')
    try {
      const { lat, lng } = await getPosition()
      const label = (await reverseLabel(lat, lng)) || t('geo.yourLocation')
      onChange({ lat, lng, label, source: 'gps' })
    } catch (e) {
      setNote(e.code === 'DENIED' ? t('geo.deniedHelp') : t(`geo.${e.code}`))
    } finally {
      setLocating(false)
    }
  }

  useEffect(() => {
    if (autoLocate && !tried.current && !value) {
      tried.current = true
      locate()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function search(e) {
    e.preventDefault()
    const q = query.trim()
    if (q.length < 3) return
    setSearching(true)
    setNote('')
    try {
      const rows = await searchPlace(q)
      setResults(rows)
      if (rows.length === 0) setNote(t('geo.noMatch'))
    } catch (err) {
      setNote(t(`geo.${err.code || 'SEARCH'}`))
    } finally {
      setSearching(false)
    }
  }

  async function pick({ lat, lng }) {
    onChange({ lat, lng, label: t('geo.selectedSpot'), source: 'map' })
    const label = await reverseLabel(lat, lng)
    if (label) onChange({ lat, lng, label, source: 'map' })
  }

  const markers = value ? [{ id: 'me', lat: value.lat, lng: value.lng, kind: 'me', title: value.label }] : []
  void ctx

  return (
    <div className="picker stack-sm">
      {title && <h3 className="picker-title">{title}</h3>}
      <button type="button" className="btn btn-primary btn-block" onClick={locate} disabled={locating}>
        {locating ? <Spinner label={t('geo.finding')} /> : <Icon name="pin" size={20} />} {t('geo.useMine')}
      </button>

      <form className="search-row" onSubmit={search}>
        <label className="sr-only" htmlFor="place-search">{t('geo.searchLabel')}</label>
        <input id="place-search" className="input" placeholder={t('geo.searchPlaceholder')} value={query} onChange={(e) => setQuery(e.target.value)} autoComplete="off" />
        <button type="submit" className="btn btn-ghost" disabled={searching || query.trim().length < 3} aria-label={t('common.search')}>
          {searching ? <Spinner label={t('common.search')} /> : <Icon name="search" size={20} />}
        </button>
      </form>

      {results.length > 0 && (
        <ul className="result-list">
          {results.map((r) => (
            <li key={`${r.lat},${r.lng}`}>
              <button type="button" onClick={() => { onChange({ lat: r.lat, lng: r.lng, label: r.label, source: 'search' }); setResults([]); setQuery('') }}>
                <Icon name="pin" size={16} /> {r.label}
              </button>
            </li>
          ))}
        </ul>
      )}

      {note && <p className="note" role="status">{note}</p>}

      {value && <p className="picked"><Icon name="check" size={16} stroke={2.4} /> <b>{value.label}</b></p>}

      {showMap && (
        <>
          <button type="button" className="link" onClick={() => setMapOpen((o) => !o)} aria-expanded={mapOpen}>
            <Icon name="map" size={16} /> {mapOpen ? t('geo.hideMap') : t('geo.pickOnMap')}
          </button>
          {mapOpen && <LazyMap height={260} markers={markers} onPick={pick} label={t('geo.mapLabel')} />}
        </>
      )}
    </div>
  )
}
