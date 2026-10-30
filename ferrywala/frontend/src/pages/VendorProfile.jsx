import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Avatar from '../components/Avatar'
import Icon from '../components/Icon'
import LazyMap from '../components/LazyMap'
import { StatusDot, Stars, Verified } from '../components/Pills'
import { EmptyState, ErrorState, ListSkeleton } from '../components/States'
import { useAuth } from '../context/Auth'
import { useCart } from '../context/Cart'
import { useToast } from '../context/Toast'
import { api } from '../lib/api'
import { money } from '../lib/format'
import { useI18n } from '../i18n'
import { useAsync } from '../lib/useAsync'
import { useLive } from '../lib/useLive'
import { useTitle } from '../lib/useTitle'

function Product({ p, vendor, canOrder, onAdd }) {
  const cart = useCart()
  const { t, pname } = useI18n()
  const qty = cart.vendorId === vendor.id ? cart.qtyOf(p.id) : 0
  const out = !p.available || p.stock <= 0
  return (
    <li className={`card product ${out ? 'out' : ''}`}>
      <div className="grow">
        <h3>{pname(p.name)}</h3>
        <div className="muted small">{money(p.price)} / {p.unit}{!out && p.stock <= 5 ? ` · ${t('vp.left', { n: p.stock })}` : ''}</div>
        {out && <span className="pill">{t('vp.out')}</span>}
      </div>
      {!out && canOrder && (
        qty > 0 ? (
          <div className="stepper" role="group" aria-label={`${pname(p.name)} ${t('vp.qty')}`}>
            <button type="button" className="icon-btn" onClick={() => cart.setQty(p.id, qty - 1)} aria-label={`${t('vp.removeOne')} ${pname(p.name)}`}><Icon name="minus" size={18} /></button>
            <b aria-live="polite">{qty}</b>
            <button type="button" className="icon-btn" onClick={() => onAdd(p)} disabled={qty >= Math.min(50, p.stock)} aria-label={`${t('vp.addOne')} ${pname(p.name)}`}><Icon name="plus" size={18} /></button>
          </div>
        ) : (
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => onAdd(p)}><Icon name="plus" size={16} /> {t('common.add')}</button>
        )
      )}
    </li>
  )
}

export default function VendorProfile() {
  const { id } = useParams()
  const { user } = useAuth()
  const cart = useCart()
  const toast = useToast()
  const nav = useNavigate()
  const { t, dist, ago } = useI18n()
  const [conflict, setConflict] = useState(null)
  const detail = useAsync((s) => api.vendor(id, s), [id])
  const reviews = useAsync((s) => api.reviewsOf(id, s), [id])
  const v = detail.data?.vendor
  useTitle(v?.shopName ?? t('vp.vendor'))

  useLive(`/topic/vendors/status`, (ev) => {
    if (String(ev.vendorId) === String(id)) detail.setData((d) => (d ? { ...d, vendor: { ...d.vendor, online: ev.online } } : d))
  })

  const canOrder = !user || user.role === 'CUSTOMER'

  function add(p) {
    if (!user) { nav('/login', { state: { from: `/vendors/${id}` } }); toast.info(t('vp.signInToRequest')); return }
    const r = cart.add(v, p)
    if (r === 'conflict') setConflict(p)
  }

  if (detail.loading && !detail.data) return <div className="page"><ListSkeleton count={3} /></div>
  if (detail.error) {
    return (
      <div className="page">
        <ErrorState error={detail.error} onRetry={detail.reload} title={detail.error.status === 404 ? t('vp.notFound') : t('vp.loadFail')} />
        <p className="center"><Link to="/vendors">{t('vp.backToVendors')}</Link></p>
      </div>
    )
  }
  const products = detail.data.products

  return (
    <div className="page stack">
      <Link to="/vendors" className="link back"><Icon name="chevron" size={16} className="flip" /> {t('vp.all')}</Link>
      <header className="card vp-head">
        <Avatar name={v.shopName} src={v.imageUrl} category={v.category} size={92} rounded={26} />
        <div className="stack-sm grow">
          <div className="row"><h1 className="h1-sm">{v.shopName}</h1>{v.verified && <Verified />}</div>
          <div className="muted">{t(`cat.${v.category}`)}{v.area ? ` · ${v.area}` : ''}</div>
          <div className="vc-meta"><StatusDot online={v.online} /><Stars value={v.rating} count={v.ratingCount} />{v.distanceKm != null && <span className="small muted">{t('common.away', { d: dist(v.distanceKm) })}</span>}</div>
          {v.description && <p>{v.description}</p>}
          {v.online && v.locationUpdatedAt && <p className="small muted">{t('vp.locShared', { when: ago(v.locationUpdatedAt) })}</p>}
        </div>
      </header>

      {conflict && (
        <div className="card note-card stack-sm" role="alertdialog" aria-label={t('vp.newReqTitle')}>
          <p>{t('vp.conflict', { name: cart.vendorName })}</p>
          <div className="row-gap">
            <button type="button" className="btn btn-primary btn-sm" onClick={() => { cart.startNew(v, conflict); setConflict(null) }}>{t('vp.startNew')}</button>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setConflict(null)}>{t('vp.keepOld')}</button>
          </div>
        </div>
      )}

      <section className="stack-sm">
        <h2 className="h2-sm">{t('nav.products')}</h2>
        {products.length === 0
          ? <EmptyState icon="tag" title={t('vp.noProducts')}>{t('vp.noProductsSub')}</EmptyState>
          : <ul className="plain stack-sm">{products.map((p) => <Product key={p.id} p={p} vendor={v} canOrder={canOrder} onAdd={add} />)}</ul>}
        {!canOrder && <p className="muted small">{t('vp.browseOnly')}</p>}
      </section>

      {Number.isFinite(v.latitude) && (
        <section className="stack-sm">
          <h2 className="h2-sm">{t('vp.where')}</h2>
          <LazyMap height={220} markers={[{ id: 'v', lat: v.latitude, lng: v.longitude, kind: 'vendor', online: v.online, label: v.shopName.slice(0, 1).toUpperCase(), title: v.shopName }]} label={t('vp.where')} />
        </section>
      )}

      <section className="stack-sm">
        <h2 className="h2-sm">{t('vp.reviews')}</h2>
        {reviews.loading && !reviews.data && <ListSkeleton count={1} />}
        {reviews.error && <ErrorState error={reviews.error} onRetry={reviews.reload} />}
        {reviews.data && reviews.data.length === 0 && <p className="muted">{t('vp.noReviews')}</p>}
        {reviews.data?.map((r) => (
          <article key={r.id} className="card flat stack-sm">
            <div className="spread"><b>{r.customerName}</b><span className="muted small">{ago(r.createdAt)}</span></div>
            <Stars value={r.rating} count={1} />
            {r.comment && <p>{r.comment}</p>}
          </article>
        ))}
      </section>

      {cart.count > 0 && cart.vendorId === v.id && (
        <Link to="/cart" className="cartbar">
          <span><Icon name="cart" size={20} /> {t('vp.items', { n: cart.count })}</span>
          <b>{money(cart.total)} · {t('vp.review')}</b>
        </Link>
      )}
    </div>
  )
}
