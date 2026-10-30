import { Link } from 'react-router-dom'
import { EmptyState } from '../components/States'
import { useI18n } from '../i18n'
import { useTitle } from '../lib/useTitle'

export default function NotFound() {
  const { t } = useI18n()
  useTitle(t('nf.title'))
  return (
    <div className="page">
      <EmptyState icon="map" title={t('nf.heading')} action={<Link className="btn btn-primary" to="/">{t('nf.home')}</Link>}>
        {t('nf.body')}
      </EmptyState>
    </div>
  )
}
