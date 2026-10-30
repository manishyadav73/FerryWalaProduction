import { useI18n } from '../i18n'
import { useOnline } from '../lib/motion'
import Icon from './Icon'

export default function OfflineBanner() {
  const { t } = useI18n()
  const online = useOnline()
  if (online) return null
  return (
    <div className="offline-banner" role="status">
      <Icon name="alert" size={18} /> {t('state.offline')}
    </div>
  )
}
