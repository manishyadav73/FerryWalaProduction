import { Link, useNavigate } from 'react-router-dom'
import Icon from '../components/Icon'
import { useAuth } from '../context/Auth'
import { useLocation2 } from '../context/Location'
import { useI18n } from '../i18n'
import { useTitle } from '../lib/useTitle'

export default function Account() {
  const { t } = useI18n()
  useTitle(t('nav.account'))
  const { user, logout } = useAuth()
  const { location, clear } = useLocation2()
  const nav = useNavigate()

  return (
    <div className="page narrow stack">
      <h1>{t('nav.account')}</h1>
      <div className="card stack-sm">
        <div className="row"><b>{user.name}</b><span className="muted">@{user.username}</span></div>
        <div className="muted">{user.email}</div>
        {!user.emailVerified && <p className="note">{t('account.unverified')}</p>}
      </div>
      <div className="card stack-sm">
        <h3>{t('account.location')}</h3>
        <p className="muted">{location ? <><Icon name="pin" size={16} /> {location.label}</> : t('account.noLocation')}</p>
        <div className="row-gap">
          <Link className="btn btn-ghost" to="/vendors">{t('account.changeLocation')}</Link>
          {location && <button type="button" className="btn btn-ghost" onClick={clear}>{t('account.forget')}</button>}
        </div>
      </div>
      <Link className="btn btn-ghost btn-block" to="/support"><Icon name="help" size={20} /> {t('account.support')}</Link>
      <button type="button" className="btn btn-ghost btn-block" onClick={() => { logout(); nav('/') }}><Icon name="logout" size={20} /> {t('common.signOut')}</button>
    </div>
  )
}
