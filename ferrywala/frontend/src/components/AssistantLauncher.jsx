import { lazy, Suspense, useEffect, useState } from 'react'
import { useI18n } from '../i18n'
import Icon from './Icon'

const Concierge = lazy(() => import('./Concierge'))
const Sahayak = lazy(() => import('./Sahayak'))

/**
 * One floating button for signed-in customers and vendors. Vendors can switch inside the panel to
 * Sahayak (the voice helper for "where should I go today"). The panel code only downloads when
 * opened, and the button is hidden on screens where it would cover the main action.
 */
export default function AssistantLauncher({ role, hidden = false }) {
  const { t } = useI18n()
  const [mode, setMode] = useState(null) // null | 'concierge' | 'sahayak'
  const [orderId, setOrderId] = useState(null)

  useEffect(() => {
    const onOpen = (e) => { setOrderId(e.detail?.orderId ?? null); setMode('concierge') }
    window.addEventListener('fw:open-concierge', onOpen)
    return () => window.removeEventListener('fw:open-concierge', onOpen)
  }, [])

  return (
    <>
      {!mode && !hidden && (
        <button type="button" className="sahayak-fab cc-fab" onClick={() => { setOrderId(null); setMode('concierge') }} aria-label={t('cc.open')}>
          <span className="fab-ring" aria-hidden="true" />
          <Icon name="sparkle" size={22} />
          <span>{t('cc.name')}</span>
        </button>
      )}
      <Suspense fallback={null}>
        {mode === 'concierge' && (
          <Concierge orderId={orderId} onClose={() => setMode(null)} onOpenSahayak={role === 'VENDOR' ? () => setMode('sahayak') : undefined} />
        )}
        {mode === 'sahayak' && <Sahayak onClose={() => setMode(null)} onBack={() => setMode('concierge')} />}
      </Suspense>
    </>
  )
}
