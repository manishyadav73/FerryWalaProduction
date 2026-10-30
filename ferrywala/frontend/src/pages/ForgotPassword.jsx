import { useState } from 'react'
import { Link } from 'react-router-dom'
import Icon from '../components/Icon'
import { useI18n } from '../i18n'
import { api } from '../lib/api'
import { useTitle } from '../lib/useTitle'

export default function ForgotPassword() {
  const { t, errMsg } = useI18n()
  useTitle(t('auth.forgotTitle'))
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')

  async function submit(e) {
    e.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await api.forgotPassword(email.trim())
      setSent(true)
    } catch (err) {
      setError(err.details?.email || errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  if (sent) {
    return (
      <div className="auth"><div className="card auth-card stack center">
        <span className="empty-icon"><Icon name="check" size={28} /></span>
        <h1>{t('auth.checkEmail')}</h1>
        <p className="muted" role="status">{t('auth.resetSent')}</p>
        <Link className="btn btn-primary" to="/login">{t('auth.backToSignIn')}</Link>
      </div></div>
    )
  }

  return (
    <div className="auth">
      <form className="card auth-card stack" onSubmit={submit} noValidate>
        <div>
          <h1>{t('auth.forgotTitle')}</h1>
          <p className="muted">{t('auth.forgotSub')}</p>
        </div>
        {error && <p className="form-error shake" role="alert" key={error}>{error}</p>}
        <label className="field">
          <span>{t('auth.email')}</span>
          <input className="input" type="email" inputMode="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" autoCapitalize="none" required />
        </label>
        <button className="btn btn-primary btn-block btn-lg" disabled={busy || !email.includes('@')} aria-busy={busy}>{busy ? t('auth.sending') : t('auth.sendLink')}</button>
        <p className="center"><Link to="/login">{t('auth.backToSignIn')}</Link></p>
      </form>
    </div>
  )
}
