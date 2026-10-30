import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/Auth'
import { Spinner } from './States'
import { useI18n } from '../i18n'

/** UX gate only. The backend independently enforces every role. */
export default function ProtectedRoute({ role, children }) {
  const { user, ready } = useAuth()
  const loc = useLocation()
  const { t } = useI18n()
  if (!ready) return <div className="center-screen"><Spinner label={t('common.checkingSession')} /></div>
  if (!user) return <Navigate to="/login" replace state={{ from: loc.pathname + loc.search }} />
  if (role && user.role !== role) return <Navigate to={homeFor(user)} replace />
  return children
}

// eslint-disable-next-line react-refresh/only-export-components
export function homeFor(user) {
  if (user?.role === 'VENDOR') return '/vendor'
  if (user?.role === 'ADMIN') return '/admin'
  return '/vendors'
}
