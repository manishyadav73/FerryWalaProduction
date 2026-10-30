import { Link, useSearchParams } from 'react-router-dom'
import Icon from '../components/Icon'
import { Spinner } from '../components/States'
import { api } from '../lib/api'
import { useAsync } from '../lib/useAsync'
import { useI18n } from '../i18n'
import { useTitle } from '../lib/useTitle'

export default function VerifyEmail() {
  const { t, errMsg } = useI18n()
  useTitle(t('verify.title'))
  const [params] = useSearchParams()
  const token = params.get('token')
  const { loading, error, data } = useAsync(() => (token ? api.verifyEmail(token) : Promise.reject(Object.assign(new Error('x'), { code: 'NO_TOKEN' }))), [token])

  return (
    <div className="auth"><div className="card auth-card stack center">
      {loading && <><Spinner label={t('verify.confirming')} /><p className="muted">{t('verify.confirming')}</p></>}
      {!loading && data && <><span className="empty-icon"><Icon name="check" size={28} /></span><h1>{t('verify.done')}</h1><p className="muted">{t('verify.doneSub')}</p><Link className="btn btn-primary" to="/login">{t('verify.continue')}</Link></>}
      {!loading && error && <><span className="empty-icon empty-error"><Icon name="alert" size={28} /></span><h1>{t('verify.problem')}</h1><p className="muted">{errMsg(error)}</p><Link className="btn btn-ghost" to="/login">{t('verify.newLink')}</Link></>}
    </div></div>
  )
}
