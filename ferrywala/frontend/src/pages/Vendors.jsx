import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Icon from '../components/Icon'
import LazyMap from '../components/LazyMap'
import LocationPicker from '../components/LocationPicker'
import { EmptyState, ErrorState, ListSkeleton } from '../components/States'
import VendorCard from '../components/VendorCard'
import { useAuth } from '../context/Auth'
import { useLocation2 } from '../context/Location'
import { useToast } from '../context/Toast'
import { api } from '../lib/api'
import { CATEGORIES } from '../lib/categories'
import { useI18n } from '../i18n'
import { useAsync } from '../lib/useAsync'
import { useLive } from '../lib/useLive'
import { useTitle } from '../lib/useTitle'

function NeedForm({ location }) {
  const { user } = useAuth()
  const toast = useToast()
  const { t, errMsg } = useI18n()
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [sent, setSent] = useState([])

  if (!user) {
    return (
      <div className="card need stack-sm">
        <h3>{t('need.notSeeing')}</h3>
        <p className="muted">{t('need.signInHelp')}</p>
        <Link className="btn btn-ghost" to="/login" state={{ from: '/vendors' }}>{t('auth.signIn')}</Link>
      </div>
    )
  }
  if (user.role !== 'CUSTOMER') return null

  async function submit(e) {
    e.preventDefault()
    const productName = name.trim()
    if (productName.length < 2) return
    setBusy(true)
    try {
      await api.sendSignal({ type: 'REQUEST', productName, latitude: location.lat, longitude: location.lng, areaName: location.label?.slice(0, 80) })
      setSent((s) => [productName, ...s].slice(0, 4))
      setName('')
      toast.success(t('need.thanks'))
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card need stack-sm" onSubmit={submit}>
      <h3>{t('need.title')}</h3>
      <p className="muted small">{t('need.sub')}</p>
      <div className="search-row">
        <label className="sr-only" htmlFor="need">{t('need.label')}</label>
        <input id="need" className="input" placeholder={t('need.placeholder')} value={name} maxLength={40} onChange={(e) => setName(e.target.value)} />
        <button className="btn btn-saffron" disabled={busy || name.trim().length < 2}>{t('need.ask')}</button>
      </div>
      {sent.length > 0 && <div className="chips">{sent.map((s) => <span key={s} className="chip chip-green"><Icon name="check" size={14} stroke={2.4} /> {s}</span>)}</div>}
    </form>
  )
}

export default function Vendors() {
  const { t } = useI18n()
  useTitle(t('vendors.title'))
  const { location, setLocation } = useLocation2()
  const [category, setCategory] = useState('')
  const [radius, setRadius] = useState(5)
  const [onlineOnly, setOnlineOnly] = useState(false)
  const [params] = useSearchParams()
  const [q, setQ] = useState(() => (params.get('q') || '').slice(0, 60))
  const [view, setView] = useState('list')
  const [changing, setChanging] = useState(false)

  const query = q.trim()
  const { loading, error, data, reload, setData } = useAsync(
    (signal) => (location
      ? api.nearby({ lat: location.lat, lng: location.lng, radiusKm: radius, category, onlineOnly, q: query.length >= 2 ? query : '' }, signal)
      : Promise.resolve([])),
    [location?.lat, location?.lng, radius, category, onlineOnly, query.length >= 2 ? query : ''],
  )

  // Live: a vendor going online/offline updates the card without a reload.
  useLive(location ? '/topic/vendors/status' : null, (ev) => {
    setData((list) => (list ? list.map((v) => (v.id === ev.vendorId ? { ...v, online: ev.online } : v)) : list))
  })

  const markers = useMemo(() => {
    const m = (data ?? []).map((v) => ({
      id: `v${v.id}`, lat: v.latitude, lng: v.longitude, kind: 'vendor', online: v.online, label: v.shopName.slice(0, 1).toUpperCase(),
      title: v.shopName, body: v.online ? t('common.onlineNow') : t('common.offline'),
    })).filter((x) => Number.isFinite(x.lat))
    if (location) m.push({ id: 'me', lat: location.lat, lng: location.lng, kind: 'me', title: t('dash.you') })
    return m
  }, [data, location, t])

  if (!location || changing) {
    return (
      <div className="page narrow stack">
        <div>
          <h1>{t('vendors.findTitle')}</h1>
          <p className="muted">{t('vendors.findSub')}</p>
        </div>
        <div className="card">
          <LocationPicker
            value={location}
            onChange={(l) => setLocation(l)}
          />
        </div>
        {location && <button type="button" className="btn btn-primary btn-lg" onClick={() => setChanging(false)}>{t('vendors.showHere')}</button>}
      </div>
    )
  }

  return (
    <div className="page stack">
      <div className="spread">
        <div>
          <h1 className="h1-sm">{t('vendors.title')}</h1>
          <button type="button" className="link" onClick={() => setChanging(true)}><Icon name="pin" size={16} /> {location.label} · {t('common.change')}</button>
        </div>
        <div className="seg" role="tablist" aria-label={t('vendors.view')}>
          <button type="button" role="tab" aria-selected={view === 'list'} className={view === 'list' ? 'on' : ''} onClick={() => setView('list')}><Icon name="list" size={16} /> {t('vendors.list')}</button>
          <button type="button" role="tab" aria-selected={view === 'map'} className={view === 'map' ? 'on' : ''} onClick={() => setView('map')}><Icon name="map" size={16} /> {t('vendors.map')}</button>
        </div>
      </div>

      <div className="filters stack-sm">
        <div className="search-row">
          <label className="sr-only" htmlFor="vq">{t('vendors.searchLabel')}</label>
          <input id="vq" className="input" type="search" placeholder={t('vendors.searchPlaceholder')} value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <div className="chips scroll-x" role="group" aria-label={t('vendors.category')}>
          <button type="button" className={`chip-btn ${category === '' ? 'on' : ''}`} aria-pressed={category === ''} onClick={() => setCategory('')}>{t('common.all')}</button>
          {CATEGORIES.map((c) => (
            <button key={c.value} type="button" className={`chip-btn ${category === c.value ? 'on' : ''}`} aria-pressed={category === c.value} onClick={() => setCategory(c.value)}>{t(`catShort.${c.value}`)}</button>
          ))}
        </div>
        <div className="row-gap">
          <label className="inline-field">{t('vendors.within')}
            <select className="input input-sm" value={radius} onChange={(e) => setRadius(Number(e.target.value))}>
              {[1, 2, 5, 10, 20].map((r) => <option key={r} value={r}>{r} {t('unit.km')}</option>)}
            </select>
          </label>
          <label className="check"><input type="checkbox" checked={onlineOnly} onChange={(e) => setOnlineOnly(e.target.checked)} /> {t('common.onlineNow')}</label>
        </div>
      </div>

      {loading && !data && <ListSkeleton />}
      {error && <ErrorState error={error} onRetry={reload} />}

      {!error && data && view === 'map' && (
        <LazyMap height={420} markers={markers} label={t('vendors.title')} />
      )}

      {!error && data && view === 'list' && (
        data.length === 0 ? (
          <EmptyState icon="store" title={t('vendors.none')}>
            {onlineOnly || category || query ? t('vendors.noneFilter') : t('vendors.noneHere')}
          </EmptyState>
        ) : (
          <div className="vendor-grid stagger">{data.map((v) => <VendorCard key={v.id} v={v} />)}</div>
        )
      )}

      <NeedForm location={location} />
    </div>
  )
}
