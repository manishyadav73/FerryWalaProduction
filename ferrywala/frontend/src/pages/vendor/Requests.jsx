import { useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../../components/Icon'
import { OrderStatusPill } from '../../components/Pills'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/States'
import { useToast } from '../../context/Toast'
import { api } from '../../lib/api'
import { money, navigateUrl } from '../../lib/format'
import { useI18n } from '../../i18n'
import { useAsync } from '../../lib/useAsync'
import { useLive } from '../../lib/useLive'
import { useTitle } from '../../lib/useTitle'

const ACTIONS = {
  PENDING: [['ACCEPTED', 'req.accept', 'btn-primary'], ['REJECTED', 'req.decline', 'btn-danger']],
  ACCEPTED: [['ON_THE_WAY', 'req.onTheWay', 'btn-primary'], ['COMPLETED', 'req.done', 'btn-ghost'], ['CANCELLED', 'common.cancel', 'btn-danger']],
  ON_THE_WAY: [['COMPLETED', 'req.done', 'btn-primary'], ['CANCELLED', 'common.cancel', 'btn-danger']],
}

function Request({ o, onChange }) {
  const toast = useToast()
  const { t, pname, ago, errMsg } = useI18n()
  const [busy, setBusy] = useState('')
  async function act(status) {
    setBusy(status)
    try { await api.vendorSetOrderStatus(o.id, status); onChange() } catch (e) { toast.error(errMsg(e)) } finally { setBusy('') }
  }
  return (
    <article className="card stack-sm order">
      <div className="spread">
        <div><b>{o.customerName}</b><div className="muted small">#{o.id} · {ago(o.createdAt)}{o.areaName ? ` · ${o.areaName}` : ''}</div></div>
        <OrderStatusPill status={o.status} />
      </div>
      <ul className="plain items">
        {o.items.map((i) => <li key={i.productId} className="spread"><span>{i.quantity} × {pname(i.name)}</span><span>{money(i.lineTotal)}</span></li>)}
      </ul>
      <div className="spread"><span className="muted">{t('orders.total')}</span><b>{money(o.totalAmount)}</b></div>
      {o.note && <p className="note">“{o.note}”</p>}
      {['ACCEPTED', 'ON_THE_WAY', 'PENDING'].includes(o.status) && (
        <a className="link" href={navigateUrl(o.latitude, o.longitude)} target="_blank" rel="noopener noreferrer"><Icon name="nav" size={16} /> {t('req.openLoc')}</a>
      )}
      {['PENDING', 'ACCEPTED', 'ON_THE_WAY'].includes(o.status) && (
        <Link className="btn btn-ghost btn-sm" to={`/meet/${o.id}`}><Icon name="pin" size={16} /> {o.status === 'PENDING' ? t('meet.linkChat') : t('meet.link')}</Link>
      )}
      {ACTIONS[o.status] && (
        <div className="row-gap">
          {ACTIONS[o.status].map(([s, label, cls]) => (
            <button key={s} type="button" className={`btn btn-sm ${cls}`} onClick={() => act(s)} disabled={!!busy}>{busy === s ? '…' : t(label)}</button>
          ))}
        </div>
      )}
    </article>
  )
}

export default function Requests() {
  const { t } = useI18n()
  useTitle(t('nav.requests'))
  const { loading, error, data, reload } = useAsync((s) => api.vendorOrders('', s), [])
  useLive('/user/queue/orders', () => reload())

  const group = (f) => (data ?? []).filter(f)
  const fresh = group((o) => o.status === 'PENDING')
  const active = group((o) => o.status === 'ACCEPTED' || o.status === 'ON_THE_WAY')
  const past = group((o) => ['COMPLETED', 'REJECTED', 'CANCELLED'].includes(o.status)).slice(0, 20)

  return (
    <div className="page narrow stack">
      <h1 className="h1-sm">{t('nav.requests')}</h1>
      {loading && !data && <ListSkeleton />}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data && data.length === 0 && <EmptyState icon="list" title={t('req.none')}>{t('req.noneSub')}</EmptyState>}
      {fresh.length > 0 && <section className="stack-sm"><h2 className="h2-sm">{t('req.new')}</h2>{fresh.map((o) => <Request key={o.id} o={o} onChange={reload} />)}</section>}
      {active.length > 0 && <section className="stack-sm"><h2 className="h2-sm">{t('req.progress')}</h2>{active.map((o) => <Request key={o.id} o={o} onChange={reload} />)}</section>}
      {past.length > 0 && <section className="stack-sm"><h2 className="h2-sm">{t('req.earlier')}</h2>{past.map((o) => <Request key={o.id} o={o} onChange={reload} />)}</section>}
    </div>
  )
}
