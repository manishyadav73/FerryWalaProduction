import { useState } from 'react'
import { Link } from 'react-router-dom'
import CountUp from '../../components/CountUp'
import Icon from '../../components/Icon'
import LazyMap from '../../components/LazyMap'
import Onboarding from '../../components/Onboarding'
import OpportunityCard from '../../components/OpportunityCard'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/States'
import { useAuth } from '../../context/Auth'
import { useToast } from '../../context/Toast'
import { useI18n } from '../../i18n'
import { api } from '../../lib/api'
import { money } from '../../lib/format'
import { useAsync } from '../../lib/useAsync'
import { useLive } from '../../lib/useLive'
import { useTitle } from '../../lib/useTitle'

function OnlineToggle({ vendor, onChange }) {
  const toast = useToast()
  const { t, errMsg } = useI18n()
  const [busy, setBusy] = useState(false)
  async function toggle() {
    setBusy(true)
    try {
      onChange(await api.vendorStatus(!vendor.online))
      toast.success(vendor.online ? t('dash.nowOffline') : t('dash.nowOnline'))
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setBusy(false)
    }
  }
  return (
    <button type="button" className={`switch ${vendor.online ? 'on' : ''}`} role="switch" aria-checked={vendor.online} onClick={toggle} disabled={busy}>
      <i aria-hidden="true" />
      <span>{vendor.online ? t('common.online') : t('common.offline')}</span>
    </button>
  )
}

export default function Dashboard() {
  const { t, pname, windowLabel, dist } = useI18n()
  useTitle(t('nav.today'))
  const { user } = useAuth()
  const toast = useToast()
  const me = useAsync((s) => api.vendorMe(s), [])
  const opp = useAsync((s) => api.vendorOpportunities({}, s), [])
  const stats = useAsync((s) => api.vendorStats(s), [])
  const products = useAsync((s) => api.vendorProducts(s), [])
  const [map, setMap] = useState(false)

  useLive('/user/queue/orders', () => { stats.reload() })

  const vendor = me.data
  const o = opp.data

  async function goOnline() {
    try { me.setData(await api.vendorStatus(true)); toast.success(t('dash.nowOnline')) } catch (e) { toast.error(e.message) }
  }

  return (
    <div className="page stack">
      <div className="spread">
        <div>
          <p className="muted">{t('dash.namaste', { name: user.name.split(' ')[0] })}</p>
          <h1 className="h1-sm">{vendor?.shopName ?? t('nav.today')}</h1>
        </div>
        {vendor && <OnlineToggle vendor={vendor} onChange={(v) => me.setData(v)} />}
      </div>

      {me.error && <ErrorState error={me.error} onRetry={me.reload} />}

      {vendor && products.data && <Onboarding me={vendor} productCount={products.data.length} onGoOnline={goOnline} />}

      {stats.data && (
        <div className="stats">
          <Link to="/vendor/requests" className={`stat ${stats.data.pending > 0 ? 'attn' : ''}`}><b><CountUp value={stats.data.pending} /></b><span>{t('dash.newRequests')}</span></Link>
          <div className="stat"><b><CountUp value={stats.data.open} /></b><span>{t('dash.open')}</span></div>
          <div className="stat"><b><CountUp value={stats.data.completedToday} /></b><span>{t('dash.doneToday')}</span></div>
          <div className="stat"><b><CountUp value={stats.data.revenueToday} format={money} /></b><span>{t('dash.today')}</span></div>
        </div>
      )}

      {opp.loading && !o && <ListSkeleton count={1} />}
      {opp.error && <ErrorState error={opp.error} onRetry={opp.reload} title={t('dash.oppFail')} />}

      {o && !o.best && (
        <EmptyState icon="map" title={t('dash.noOpp')}>
          {t('dash.noOppSub')}
        </EmptyState>
      )}

      {o?.best && <OpportunityCard o={o.best} featured />}
      {o && <p className="muted small">{t('dash.basedOn', { n: o.dataQuality?.signalsConsidered ?? 0, d: o.dataQuality?.windowDays ?? 30 })}</p>}

      {o && o.others.length > 0 && (
        <section className="stack-sm">
          <div className="spread">
            <h2 className="h2-sm">{t('dash.others')}</h2>
            <button type="button" className="link" onClick={() => setMap((m) => !m)}><Icon name="map" size={16} /> {map ? t('geo.hideMap') : t('dash.showMap')}</button>
          </div>
          {map && (
            <LazyMap height={300} label={t('dash.others')} markers={[
              ...(o.vendorLatitude != null ? [{ id: 'me', lat: o.vendorLatitude, lng: o.vendorLongitude, kind: 'me', title: t('dash.you') }] : []),
              ...[o.best, ...o.others].filter(Boolean).map((a, i) => ({ id: a.areaKey, lat: a.latitude, lng: a.longitude, kind: 'demand', label: String(i + 1), title: a.areaName, body: `${t('dash.score')} ${a.score}` })),
            ]} />
          )}
          <ul className="plain stack-sm">
            {o.others.map((a) => (
              <li key={a.areaKey} className="card flat spread">
                <div>
                  <b>{a.areaName}</b>
                  <div className="muted small">{a.topProducts.slice(0, 3).map((p) => pname(p.name)).join(', ')} · {windowLabel(a.recommendedWindow)}</div>
                </div>
                <span className="muted small">{a.distanceKm != null ? dist(a.distanceKm) : ''}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
