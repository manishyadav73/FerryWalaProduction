import { useState } from 'react'
import Dialog from './Dialog'
import { useToast } from '../context/Toast'
import { useI18n } from '../i18n'
import { api } from '../lib/api'

import { CASE_CATEGORIES } from '../lib/concierge'

/** Opens a support case. The user always sees and can edit what will be sent. */
export default function CaseDialog({ orderId, category = 'OTHER', summary = '', source = 'APP', onClose, onCreated }) {
  const { t, errMsg } = useI18n()
  const toast = useToast()
  const [cat, setCat] = useState(CASE_CATEGORIES.includes(category) ? category : 'OTHER')
  const [text, setText] = useState(summary)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(e) {
    e.preventDefault()
    if (text.trim().length < 5) { setError(t('cc.caseTooShort')); return }
    setBusy(true)
    setError('')
    try {
      const c = await api.createCase({ category: cat, orderId: orderId || undefined, summary: text.trim(), source })
      toast.success(t('cc.caseCreated', { id: c.id }))
      onCreated?.(c)
      onClose()
    } catch (err) {
      setError(errMsg(err))
      setBusy(false)
    }
  }

  return (
    <Dialog title={t('cc.caseTitle')} onClose={onClose} busy={busy}>
      <form className="stack-sm" onSubmit={submit} noValidate>
        <p className="muted small">{t('cc.caseSub')}</p>
        <label className="field">
          <span>{t('cc.caseAbout')}</span>
          <select className="input" value={cat} onChange={(e) => setCat(e.target.value)}>
            {CASE_CATEGORIES.map((c) => <option key={c} value={c}>{t(`case.cat.${c}`)}</option>)}
          </select>
        </label>
        <label className="field">
          <span>{t('cc.caseWhat')}</span>
          <textarea className="input" maxLength={1000} value={text} onChange={(e) => setText(e.target.value)} aria-invalid={!!error} aria-describedby={error ? 'case-err' : undefined} />
        </label>
        {error && <p id="case-err" className="field-error" role="alert">{error}</p>}
        <div className="row-gap">
          <button type="submit" className="btn btn-primary" disabled={busy}>{busy ? t('common.sending') : t('cc.caseSubmit')}</button>
          <button type="button" className="btn btn-ghost" onClick={onClose} disabled={busy}>{t('cc.notNow')}</button>
        </div>
      </form>
    </Dialog>
  )
}
