import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Icon from '../components/Icon'
import LocationPicker from '../components/LocationPicker'
import { EmptyState } from '../components/States'
import { RequestSteps } from '../components/RequestSteps'
import { useCart } from '../context/Cart'
import { useLocation2 } from '../context/Location'
import { useToast } from '../context/Toast'
import { api } from '../lib/api'
import { money } from '../lib/format'
import { useI18n } from '../i18n'
import { useTitle } from '../lib/useTitle'

export default function Cart() {
  const { t, pname, errMsg } = useI18n()
  useTitle(t('cart.title'))
  const cart = useCart()
  const { location, setLocation } = useLocation2()
  const toast = useToast()
  const nav = useNavigate()
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [editLoc, setEditLoc] = useState(false)

  if (cart.items.length === 0) {
    return (
      <div className="page narrow">
        <EmptyState icon="cart" title={t('cart.empty')} action={<Link className="btn btn-primary" to="/vendors">{t('cart.findVendors')}</Link>}>
          {t('cart.emptySub')}
        </EmptyState>
      </div>
    )
  }

  async function send() {
    if (!location) { setEditLoc(true); setError(t('cart.chooseWhere')); return }
    setBusy(true)
    setError('')
    try {
      await api.placeOrder({
        vendorId: cart.vendorId,
        items: cart.items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        note: note.trim() || undefined,
        latitude: location.lat,
        longitude: location.lng,
        areaName: location.label?.slice(0, 80),
      })
      cart.clear()
      toast.success(t('cart.sent'))
      nav('/orders', { state: { sent: true } })
    } catch (e) {
      setError(errMsg(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page narrow stack">
      <RequestSteps step={busy ? 2 : location && !editLoc ? 2 : 1} />
      <div>
        <h1 className="h1-sm">{t('cart.title')}</h1>
        <p className="muted">{t('cart.to')} <Link to={`/vendors/${cart.vendorId}`}>{cart.vendorName}</Link></p>
      </div>

      <ul className="plain stack-sm">
        {cart.items.map((i) => (
          <li key={i.productId} className="card product">
            <div className="grow">
              <h3>{pname(i.name)}</h3>
              <div className="muted small">{money(i.price)} / {i.unit}</div>
            </div>
            <div className="stepper" role="group" aria-label={`${pname(i.name)} ${t('vp.qty')}`}>
              <button type="button" className="icon-btn" onClick={() => cart.setQty(i.productId, i.quantity - 1)} aria-label={`${t('vp.removeOne')} ${pname(i.name)}`}><Icon name={i.quantity === 1 ? 'trash' : 'minus'} size={18} /></button>
              <b aria-live="polite" key={i.quantity} className="bump">{i.quantity}</b>
              <button type="button" className="icon-btn" onClick={() => cart.setQty(i.productId, i.quantity + 1)} disabled={i.quantity >= i.max} aria-label={`${t('vp.addOne')} ${pname(i.name)}`}><Icon name="plus" size={18} /></button>
            </div>
            <b className="line-total">{money(Number(i.price) * i.quantity)}</b>
          </li>
        ))}
      </ul>

      <div className="card stack-sm">
        <h3>{t('cart.where')}</h3>
        {!editLoc && location ? (
          <div className="spread"><span><Icon name="pin" size={16} /> {location.label}</span><button type="button" className="link" onClick={() => setEditLoc(true)}>{t('common.change')}</button></div>
        ) : (
          <LocationPicker value={location} onChange={(l) => { setLocation(l) }} />
        )}
        {editLoc && location && <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditLoc(false)}>{t('common.done')}</button>}
        <label className="field"><span>{t('cart.note')}</span>
          <textarea className="input" maxLength={300} value={note} onChange={(e) => setNote(e.target.value)} placeholder={t('cart.notePlaceholder')} />
        </label>
      </div>

      <div className="card stack-sm">
        <div className="spread"><span>{t('cart.total')}</span><b className="total">{money(cart.total)}</b></div>
        <p className="muted small">{t('cart.payNote')}</p>
        {error && <p className="form-error shake" role="alert" key={error}>{error}</p>}
        <button type="button" className="btn btn-primary btn-lg btn-block" onClick={send} disabled={busy} aria-busy={busy}>{busy ? t('common.sending') : t('cart.send')}</button>
        <button type="button" className="link center-link" onClick={cart.clear}>{t('cart.clear')}</button>
      </div>
    </div>
  )
}
