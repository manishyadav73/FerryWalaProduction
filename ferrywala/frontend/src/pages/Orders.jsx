import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import SuccessCheck from '../components/SuccessCheck'
import Avatar from '../components/Avatar'
import Icon from '../components/Icon'
import { OrderStatusPill } from '../components/Pills'
import { EmptyState, ErrorState, ListSkeleton } from '../components/States'
import { useToast } from '../context/Toast'
import { useI18n } from '../i18n'
import { api } from '../lib/api'
import { money } from '../lib/format'
import { useAsync } from '../lib/useAsync'
import { useLive } from '../lib/useLive'
import { useTitle } from '../lib/useTitle'

const STEPS = ['PENDING', 'ACCEPTED', 'ON_THE_WAY', 'COMPLETED']

function Timeline({ status }) {
  const { t } = useI18n()
  if (status === 'REJECTED' || status === 'CANCELLED') return null
  const at = STEPS.indexOf(status)
  return (
    <ol className="timeline" aria-label={t('orders.progress')}>
      {STEPS.map((s, i) => (
        <li key={s} className={i <= at ? 'done' : ''} aria-current={i === at ? 'step' : undefined}><i /><span>{t(`step.${s}`)}</span></li>
      ))}
    </ol>
  )
}

function ReviewForm({ order, onDone }) {
  const toast = useToast()
  const { t, errMsg } = useI18n()
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(e) {
    e.preventDefault()
    if (!rating) return
    setBusy(true)
    try {
      await api.reviewOrder(order.id, { rating, comment: comment.trim() || undefined })
      toast.success(t('orders.thanksReview'))
      onDone()
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form className="stack-sm review-form" onSubmit={submit}>
      <b>{t('orders.howWas', { name: order.vendorName })}</b>
      <div className="rate" role="radiogroup" aria-label={t('orders.rating')}>
        {[1, 2, 3, 4, 5].map((n) => (
          <button key={n} type="button" role="radio" aria-checked={rating === n} aria-label={`${n}`} className={n <= rating ? 'on' : ''} onClick={() => setRating(n)}>
            <Icon name="star" size={28} />
          </button>
        ))}
      </div>
      <textarea className="input" maxLength={500} placeholder={t('orders.comment')} value={comment} onChange={(e) => setComment(e.target.value)} />
      <button className="btn btn-primary btn-sm" disabled={!rating || busy}>{busy ? t('common.sending') : t('orders.submitReview')}</button>
    </form>
  )
}

function OrderCard({ o, onChange }) {
  const toast = useToast()
  const { t, pname, ago, errMsg } = useI18n()
  const [busy, setBusy] = useState(false)
  const [open, setOpen] = useState(false)
  const cancellable = o.status === 'PENDING' || o.status === 'ACCEPTED'

  async function cancel() {
    setBusy(true)
    try { await api.cancelOrder(o.id); toast.info(t('orders.cancelled')); onChange() } catch (e) { toast.error(errMsg(e)) } finally { setBusy(false) }
  }

  return (
    <article className="card stack-sm order">
      <div className="row">
        <Avatar name={o.vendorName} src={o.vendorImageUrl} size={44} rounded={14} />
        <div className="grow">
          <Link to={`/vendors/${o.vendorId}`}><b>{o.vendorName}</b></Link>
          <div className="muted small">#{o.id} · {ago(o.createdAt)}</div>
        </div>
        <OrderStatusPill status={o.status} />
      </div>
      <Timeline status={o.status} />
      <ul className="plain items">
        {o.items.map((i) => <li key={i.productId} className="spread"><span>{i.quantity} × {pname(i.name)} <span className="muted small">({i.unit})</span></span><span>{money(i.lineTotal)}</span></li>)}
      </ul>
      <div className="spread"><span className="muted">{t('orders.total')}</span><b>{money(o.totalAmount)}</b></div>
      {o.note && <p className="muted small">{t('orders.note')}: {o.note}</p>}
      {o.status === 'REJECTED' && <p className="note">{t('orders.rejected')}</p>}
      {o.status === 'COMPLETED' && !o.reviewed && (
        open ? <ReviewForm order={o} onDone={onChange} /> : <button type="button" className="btn btn-ghost btn-sm" onClick={() => setOpen(true)}><Icon name="star" size={16} /> {t('orders.rate')}</button>
      )}
      {['PENDING', 'ACCEPTED', 'ON_THE_WAY'].includes(o.status) && (
        <Link className="btn btn-ghost btn-sm" to={`/meet/${o.id}`}><Icon name="pin" size={16} /> {o.status === 'PENDING' ? t('meet.linkChat') : t('meet.link')}</Link>
      )}
      {cancellable && <button type="button" className="btn btn-danger btn-sm" onClick={cancel} disabled={busy}>{busy ? t('orders.cancelling') : t('orders.cancel')}</button>}
    </article>
  )
}

export default function Orders() {
  const { t } = useI18n()
  useTitle(t('orders.title'))
  const { loading, error, data, reload } = useAsync((s) => api.myOrders(s), [])
  const { state } = useLocation()
  const [banner, setBanner] = useState(!!state?.sent)
  // Silent refresh when the vendor responds.
  useLive('/user/queue/orders', () => reload())

  return (
    <div className="page narrow stack">
      <h1 className="h1-sm">{t('orders.title')}</h1>
      {banner && (
        <div className="card success-card" role="status">
          <SuccessCheck />
          <div className="grow"><b>{t('cart.sent')}</b><p className="muted small">{t('orders.sentSub')}</p></div>
          <button type="button" className="icon-btn" onClick={() => setBanner(false)} aria-label={t('common.close')}><Icon name="x" /></button>
        </div>
      )}
      {loading && !data && <ListSkeleton />}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data && data.length === 0 && (
        <EmptyState icon="receipt" title={t('orders.none')} action={<Link className="btn btn-primary" to="/vendors">{t('cart.findVendors')}</Link>}>
          {t('orders.noneSub')}
        </EmptyState>
      )}
      {data?.map((o) => <OrderCard key={o.id} o={o} onChange={reload} />)}
    </div>
  )
}
