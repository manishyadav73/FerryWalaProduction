import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import Icon from '../components/Icon'
import { homeFor } from '../components/ProtectedRoute'
import { useAuth } from '../context/Auth'
import { useToast } from '../context/Toast'
import { useI18n } from '../i18n'
import { useTitle } from '../lib/useTitle'

export default function Login() {
  const { t, errMsg } = useI18n()
  useTitle(t('auth.signIn'))
  const { user, login } = useAuth()
  const toast = useToast()
  const nav = useNavigate()
  const { state } = useLocation()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [show, setShow] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  if (user) return <Navigate to={state?.from || homeFor(user)} replace />

  async function submit(e) {
    e.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    try {
      const res = await login(identifier.trim(), password)
      nav(state?.from || homeFor(res.user), { replace: true })
    } catch (err) {
      setError(errMsg(err))
      if (err.code === 'NETWORK') toast.error(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <form className="card auth-card stack" onSubmit={submit} noValidate>
        <div>
          <h1>{t('auth.welcomeBack')}</h1>
          <p className="muted">{t('auth.signInSub')}</p>
        </div>
        {error && <p className="form-error shake" role="alert" key={error}>{error}</p>}
        <label className="field">
          <span>{t('auth.identifier')}</span>
          <input className="input" value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoComplete="username" autoCapitalize="none" required />
        </label>
        <label className="field">
          <span>{t('auth.password')}</span>
          <span className="input-wrap">
            <input className="input" type={show ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
            <button type="button" className="icon-btn" onClick={() => setShow((s) => !s)} aria-label={show ? t('auth.hidePw') : t('auth.showPw')}><Icon name={show ? 'eyeoff' : 'eye'} size={20} /></button>
          </span>
        </label>
        <p className="small"><Link to="/forgot-password">{t('auth.forgot')}</Link></p>
        <button className="btn btn-primary btn-block btn-lg" disabled={busy || !identifier || !password} aria-busy={busy}>{busy ? t('auth.signingIn') : t('auth.signIn')}</button>
        <p className="center muted">{t('auth.newHere')} <Link to="/register">{t('auth.createAccount')}</Link> {t('auth.or')} <Link to="/register?role=VENDOR">{t('auth.joinAsVendor')}</Link></p>
      </form>
    </div>
  )
}
