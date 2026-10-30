import { useEffect, useRef } from 'react'
import { useI18n } from '../i18n'
import Icon from './Icon'

/**
 * Small accessible modal: role=dialog, labelled, Escape closes, focus moves in and returns on
 * close, Tab stays inside. Used for every "please confirm" step, so nothing consequential
 * happens from a single stray tap.
 */
export default function Dialog({ title, onClose, children, footer, busy = false }) {
  const { t } = useI18n()
  const box = useRef(null)
  const returnTo = useRef(typeof document !== 'undefined' ? document.activeElement : null)

  useEffect(() => {
    const node = box.current
    const first = node?.querySelector('textarea, input, select, button:not([data-dialog-close])') ?? node?.querySelector('button')
    first?.focus()
    const back = returnTo.current
    const onKey = (e) => {
      if (e.key === 'Escape' && !busy) { e.stopPropagation(); onClose() }
      if (e.key !== 'Tab' || !node) return
      const items = node.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled])')
      if (!items.length) return
      const a = items[0]
      const z = items[items.length - 1]
      if (e.shiftKey && document.activeElement === a) { e.preventDefault(); z.focus() }
      else if (!e.shiftKey && document.activeElement === z) { e.preventDefault(); a.focus() }
    }
    document.addEventListener('keydown', onKey, true)
    return () => {
      document.removeEventListener('keydown', onKey, true)
      if (back && typeof back.focus === 'function') back.focus()
    }
  }, [onClose, busy])

  return (
    <div className="dlg-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget && !busy) onClose() }}>
      <div className="dlg" role="dialog" aria-modal="true" aria-label={title} ref={box}>
        <div className="dlg-head">
          <h2>{title}</h2>
          <button type="button" className="icon-btn" data-dialog-close onClick={onClose} disabled={busy} aria-label={t('common.close')}><Icon name="x" /></button>
        </div>
        <div className="dlg-body stack-sm">{children}</div>
        {footer && <div className="dlg-foot">{footer}</div>}
      </div>
    </div>
  )
}
