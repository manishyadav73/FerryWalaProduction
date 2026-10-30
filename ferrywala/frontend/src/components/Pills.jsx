import Icon from './Icon'
import { useI18n } from '../i18n'

export function StatusDot({ online, label }) {
  const { t } = useI18n()
  return (
    <span className={`status-dot ${online ? 'on' : 'off'}`}>
      <i aria-hidden="true" />
      {label ?? (online ? t('common.onlineNow') : t('common.offline'))}
    </span>
  )
}

export function Verified() {
  const { t } = useI18n()
  return <span className="verified" title={t('common.verifiedBy')}><Icon name="shield" size={14} stroke={2} /> {t('common.verified')}</span>
}

export function Stars({ value = 0, count, size = 14 }) {
  const { t } = useI18n()
  if (!count) return <span className="muted small">{t('common.new')}</span>
  return (
    <span className="stars" aria-label={t('common.rated', { v: value, n: count })}>
      <Icon name="star" size={size} className="star-on" /> <b>{value.toFixed(1)}</b> <span className="muted">({count})</span>
    </span>
  )
}

export function OrderStatusPill({ status }) {
  const { t } = useI18n()
  return <span className={`pill pill-${status.toLowerCase()}`}>{t(`status.${status}`)}</span>
}
