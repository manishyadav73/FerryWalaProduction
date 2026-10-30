import Icon from './Icon'
import { useI18n } from '../i18n'

export function Skeleton({ h = 16, w = '100%', r = 10, style }) {
  return <span className="skeleton" style={{ height: h, width: w, borderRadius: r, ...style }} aria-hidden="true" />
}

export function CardSkeleton({ rows = 3 }) {
  const { t } = useI18n()
  return (
    <div className="card stack-sm" aria-busy="true" aria-label={t('common.loading')}>
      <Skeleton h={20} w="55%" />
      {Array.from({ length: rows }).map((_, i) => <Skeleton key={i} h={14} w={`${90 - i * 12}%`} />)}
    </div>
  )
}

export function ListSkeleton({ count = 3 }) {
  const { t } = useI18n()
  return <div className="stack" role="status" aria-label={t('common.loading')}>{Array.from({ length: count }).map((_, i) => <CardSkeleton key={i} />)}</div>
}

export function EmptyState({ icon = 'leaf', title, children, action }) {
  return (
    <div className="empty">
      <span className="empty-icon"><Icon name={icon} size={28} /></span>
      <h3>{title}</h3>
      {children && <p className="muted">{children}</p>}
      {action}
    </div>
  )
}

export function ErrorState({ error, onRetry, title }) {
  const { t, errMsg } = useI18n()
  return (
    <div className="empty empty-error" role="alert">
      <span className="empty-icon"><Icon name="alert" size={28} /></span>
      <h3>{title || t('common.loadFailed')}</h3>
      <p className="muted">{error ? errMsg(error) : t('common.tryAgainLater')}</p>
      {onRetry && <button type="button" className="btn btn-ghost" onClick={onRetry}><Icon name="refresh" size={18} /> {t('common.tryAgain')}</button>}
    </div>
  )
}

export function Spinner({ label }) {
  const { t } = useI18n()
  return <span className="spinner" role="status" aria-label={label || t('common.loading')} />
}
