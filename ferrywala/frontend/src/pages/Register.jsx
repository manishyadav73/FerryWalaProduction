import { useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import Icon from '../components/Icon'
import { homeFor } from '../components/ProtectedRoute'
import { useAuth } from '../context/Auth'
import { CATEGORIES } from '../lib/categories'
import { useI18n } from '../i18n'
import { useTitle } from '../lib/useTitle'

function strength(pw) {
  let s = 0
  if (pw.length >= 8) s++
  if (/[A-Za-z]/.test(pw) && /\d/.test(pw)) s++
  if (pw.length >= 12 || /[^A-Za-z0-9]/.test(pw)) s++
  return s
}

export default function Register() {
  const { t, errMsg } = useI18n()
  useTitle(t('auth.createAccount'))
  const { user, register } = useAuth()
  const nav = useNavigate()
  const [params] = useSearchParams()
  const [role, setRole] = useState(params.get('role') === 'VENDOR' ? 'VENDOR' : 'CUSTOMER')
  const [f, setF] = useState({ name: '', username: '', email: '', password: '', shopName: '', category: '' })
  const [show, setShow] = useState(false)
  const [errors, setErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(null)

  if (user && !done) return <Navigate to={homeFor(user)} replace />
  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))
  const vendor = role === 'VENDOR'

  function validate() {
    const e = {}
    if (f.name.trim().length < 2) e.name = t('auth.vName')
    if (!/^[A-Za-z0-9_.]{3,24}$/.test(f.username.trim())) e.username = t('auth.vUsername')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) e.email = t('auth.vEmail')
    if (f.password.length < 8 || !/[A-Za-z]/.test(f.password) || !/\d/.test(f.password)) e.password = t('auth.vPassword')
    if (vendor && f.shopName.trim().length < 2) e.shopName = t('auth.vShop')
    if (vendor && !f.category) e.category = t('auth.vCategory')
    return e
  }

  async function submit(ev) {
    ev.preventDefault()
    if (busy) return
    const e = validate()
    setErrors(e)
    setFormError('')
    if (Object.keys(e).length) return
    setBusy(true)
    try {
      const res = await register({
        name: f.name.trim(), username: f.username.trim(), email: f.email.trim(), password: f.password, role,
        ...(vendor ? { shopName: f.shopName.trim(), category: f.category } : {}),
      })
      if (res.token) nav(vendor ? '/vendor/profile?welcome=1' : '/vendors', { replace: true })
      else setDone(res.user)
    } catch (err) {
      if (err.details && Object.keys(err.details).length) {
        const d = { ...err.details }
        if (d.email && err.code.includes('EMAIL')) d.email = t('err.EMAIL_TAKEN')
        if (d.username && err.code.includes('USERNAME')) d.username = t('err.USERNAME_TAKEN')
        setErrors(d)
      } else setFormError(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <div className="auth"><div className="card auth-card stack center">
        <span className="empty-icon"><Icon name="check" size={28} /></span>
        <h1>{t('auth.checkEmail')}</h1>
        <p className="muted">{t('auth.sentLinkTo')} <b>{done.email}</b>. {t('auth.openThenSignIn')}</p>
        <Link className="btn btn-primary" to="/login">{t('auth.goSignIn')}</Link>
      </div></div>
    )
  }

  const s = strength(f.password)
  return (
    <div className="auth">
      <form className="card auth-card stack" onSubmit={submit} noValidate>
        <div>
          <h1>{t('auth.createTitle')}</h1>
          <p className="muted">{t('auth.underMinute')}</p>
        </div>

        <div className="role-toggle" role="radiogroup" aria-label={t('auth.iAm')}>
          <button type="button" role="radio" aria-checked={!vendor} className={!vendor ? 'on' : ''} onClick={() => setRole('CUSTOMER')}>
            <Icon name="user" /> <b>{t('auth.customer')}</b><span>{t('auth.customerSub')}</span>
          </button>
          <button type="button" role="radio" aria-checked={vendor} className={vendor ? 'on' : ''} onClick={() => setRole('VENDOR')}>
            <Icon name="store" /> <b>{t('auth.vendor')}</b><span>{t('auth.vendorSub')}</span>
          </button>
        </div>

        {formError && <p className="form-error shake" role="alert" key={formError}>{formError}</p>}

        <label className="field"><span>{t('auth.name')}</span>
          <input className="input" value={f.name} onChange={set('name')} autoComplete="name" aria-invalid={!!errors.name} />
          {errors.name && <em className="field-error">{errors.name}</em>}
        </label>
        <label className="field"><span>{t('auth.username')}</span>
          <input className="input" value={f.username} onChange={set('username')} autoComplete="username" autoCapitalize="none" aria-invalid={!!errors.username} />
          {errors.username && <em className="field-error">{errors.username}</em>}
        </label>
        <label className="field"><span>{t('auth.email')}</span>
          <input className="input" type="email" inputMode="email" value={f.email} onChange={set('email')} autoComplete="email" autoCapitalize="none" aria-invalid={!!errors.email} />
          {errors.email && <em className="field-error">{errors.email}</em>}
        </label>
        <label className="field"><span>{t('auth.password')}</span>
          <span className="input-wrap">
            <input className="input" type={show ? 'text' : 'password'} value={f.password} onChange={set('password')} autoComplete="new-password" aria-invalid={!!errors.password} />
            <button type="button" className="icon-btn" onClick={() => setShow((x) => !x)} aria-label={show ? t('auth.hidePw') : t('auth.showPw')}><Icon name={show ? 'eyeoff' : 'eye'} size={20} /></button>
          </span>
          {f.password && <span className={`meter meter-${s}`} aria-hidden="true"><i /><i /><i /></span>}
          {errors.password && <em className="field-error">{errors.password}</em>}
        </label>

        {vendor && (
          <>
            <label className="field"><span>{t('auth.shopName')}</span>
              <input className="input" value={f.shopName} onChange={set('shopName')} placeholder={t('auth.shopPlaceholder')} aria-invalid={!!errors.shopName} />
              {errors.shopName && <em className="field-error">{errors.shopName}</em>}
            </label>
            <fieldset className="field">
              <legend>{t('auth.whatSell')}</legend>
              <div className="chips">
                {CATEGORIES.map((c) => (
                  <button key={c.value} type="button" className={`chip-btn ${f.category === c.value ? 'on' : ''}`} onClick={() => setF((x) => ({ ...x, category: c.value }))} aria-pressed={f.category === c.value}>{t(`cat.${c.value}`)}</button>
                ))}
              </div>
              {errors.category && <em className="field-error">{errors.category}</em>}
            </fieldset>
          </>
        )}

        <button className="btn btn-primary btn-block btn-lg" disabled={busy} aria-busy={busy}>{busy ? t('auth.creating') : t('auth.createAccount')}</button>
        <p className="center muted">{t('auth.haveAccount')} <Link to="/login">{t('auth.signIn')}</Link></p>
      </form>
    </div>
  )
}
