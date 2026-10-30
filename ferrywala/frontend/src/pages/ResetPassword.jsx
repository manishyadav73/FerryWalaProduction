import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Icon from '../components/Icon'
import { useI18n } from '../i18n'
import { api } from '../lib/api'
import { useTitle } from '../lib/useTitle'

export default function ResetPassword() {
  const { t, errMsg } = useI18n()
  useTitle(t('auth.resetTitle'))
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [password, setPassword] = useState('')
  const [show, setShow] = useState(false)
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')

  const valid = password.length >= 8 && /[A-Za-z]/.test(password) && /\d/.test(password)

  async function submit(e) {
    e.preventDefault()
    if (busy || !valid) return
    setBusy(true)
    setError('')
    try {
      await api.resetPassword(token, password)
      setDone(true)
    } catch (err) {
      if (err.code === 'INVALID_TOKEN') setError(t('err.INVALID_TOKEN_RESET'))
      else setError(err.details?.password || errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  if (!token) {
    return (
      <div className="auth"><div className="card auth-card stack center">
        <h1>{t('auth.resetTitle')}</h1>
        <p className="form-error" role="alert">{t('auth.resetMissing')}</p>
        <Link className="btn btn-primary" to="/forgot-password">{t('auth.sendLink')}</Link>
      </div></div>
    )
  }

  if (done) {
    return (
      <div className="auth"><div className="card auth-card stack center">
        <span className="empty-icon"><Icon name="check" size={28} /></span>
        <h1>{t('auth.resetDone')}</h1>
        <Link className="btn btn-primary" to="/login">{t('auth.goSignIn')}</Link>
      </div></div>
    )
  }

  return (
    <div className="auth">
      <form className="card auth-card stack" onSubmit={submit} noValidate>
        <h1>{t('auth.resetTitle')}</h1>
        {error && <p className="form-error shake" role="alert" key={error}>{error}</p>}
        <label className="field">
          <span>{t('auth.newPassword')}</span>
          <span className="input-wrap">
            <input className="input" type={show ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" aria-describedby="pw-hint" required />
            <button type="button" className="icon-btn" onClick={() => setShow((s) => !s)} aria-label={show ? t('auth.hidePw') : t('auth.showPw')}><Icon name={show ? 'eyeoff' : 'eye'} size={20} /></button>
          </span>
          <small id="pw-hint" className="muted">{t('auth.vPassword')}</small>
        </label>
        <button className="btn btn-primary btn-block btn-lg" disabled={busy || !valid} aria-busy={busy}>{busy ? t('auth.resetSaving') : t('auth.resetSave')}</button>
      </form>
    </div>
  )
}
