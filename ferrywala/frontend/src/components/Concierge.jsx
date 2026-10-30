import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/Auth'
import { useToast } from '../context/Toast'
import { useI18n } from '../i18n'
import { api } from '../lib/api'
import { navigateUrl } from '../lib/format'
import CaseDialog from './CaseDialog'
import Dialog from './Dialog'
import Icon from './Icon'

let seq = 0
const nextId = () => `m${Date.now()}-${seq++}`

function Card({ card }) {
  return (
    <div className="cc-card" role="group" aria-label={card.title}>
      <b>{card.title}</b>
      <ul>{card.lines.map((l) => <li key={l}>{l}</li>)}</ul>
    </div>
  )
}

/**
 * The FerryWala AI Concierge panel. It only displays what the server composed and proposes
 * actions as buttons. Every action opens a confirmation first and then calls the normal API,
 * so the assistant can never do more than the signed-in user could do themselves.
 */
export default function Concierge({ onClose, orderId, onOpenSahayak }) {
  const { t, lang } = useI18n()
  const { user } = useAuth()
  const toast = useToast()
  const nav = useNavigate()
  const [msgs, setMsgs] = useState([])
  const [status, setStatus] = useState(null)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [failed, setFailed] = useState(null)
  const [pending, setPending] = useState(null) // action waiting for the user's confirmation
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const end = useRef(null)
  const input = useRef(null)

  const firstName = (user?.name || '').split(' ')[0]

  useEffect(() => {
    const c = new AbortController()
    Promise.allSettled([api.conciergeStatus(c.signal), api.conciergeHistory(c.signal)]).then(([s, h]) => {
      if (c.signal.aborted) return
      if (s.status === 'fulfilled') setStatus(s.value)
      if (h.status === 'fulfilled') setMsgs(h.value.map((m) => ({ id: `h${m.id}`, who: m.author === 'USER' ? 'me' : 'bot', text: m.text })))
    })
    return () => c.abort()
  }, [])

  useEffect(() => { input.current?.focus() }, [])
  useEffect(() => { end.current?.scrollIntoView?.({ block: 'end' }) }, [msgs, busy, failed])
  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape' && !pending) onClose() }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose, pending])

  const ask = useCallback(async (message, { repeat = false } = {}) => {
    const q = message.trim()
    if (!q || busy) return
    setFailed(null)
    if (!repeat) setMsgs((m) => [...m, { id: nextId(), who: 'me', text: q }])
    setText('')
    setBusy(true)
    try {
      const res = await api.conciergeChat({ message: q, orderId: orderId || undefined, language: lang })
      setMsgs((m) => [...m, { id: nextId(), who: 'bot', text: res.reply.text, reply: res.reply, provider: res.provider }])
    } catch (e) {
      setFailed({ q, message: e.code === 'RATE_LIMITED' || e.code === 'FORBIDDEN' ? e.message : null })
    } finally {
      setBusy(false)
    }
  }, [busy, orderId, lang])

  async function clear() {
    if (!window.confirm(t('cc.clearConfirm'))) return
    try { await api.conciergeClear(); setMsgs([]); toast.info(t('cc.cleared')) } catch (e) { toast.error(e.message) }
  }

  function run(action) {
    const p = action.payload || {}
    switch (action.kind) {
      case 'OPEN_MEETING':
      case 'START_SHARE':
        onClose()
        nav(`/meet/${p.orderId}${action.kind === 'START_SHARE' ? '?share=1' : ''}`)
        break
      case 'OPEN_VENDORS':
        onClose()
        nav('/vendors')
        break
      case 'OPEN_DIRECTIONS':
        api.meeting(p.orderId)
          .then((m) => window.open(navigateUrl(m.latitude, m.longitude), '_blank', 'noopener,noreferrer'))
          .catch((e) => toast.error(e.message))
        break
      case 'SEND_MESSAGE':
        setDraft(String(p.draft || ''))
        setPending(action)
        break
      default:
        setPending(action) // SEND_MEETING_UPDATE, CREATE_CASE
    }
  }

  async function confirm() {
    const p = pending.payload || {}
    setSending(true)
    try {
      if (pending.kind === 'SEND_MEETING_UPDATE') {
        await api.meetingUpdate(p.orderId, { type: p.type, etaMinutes: p.etaMinutes, source: 'CONCIERGE' })
      } else if (pending.kind === 'SEND_MESSAGE') {
        if (!draft.trim()) return
        await api.sendMessage(p.orderId, draft.trim())
      }
      toast.success(t('cc.sentOk'))
      setPending(null)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setSending(false)
    }
  }

  const lastUser = [...msgs].reverse().find((m) => m.who === 'me')?.text || ''
  const suggestions = [...new Set(msgs.filter((m) => m.reply).slice(-1)[0]?.reply.suggestions
    ?? (user?.role === 'VENDOR' ? ['requests', 'meeting', 'late', 'cant_find', 'human'] : ['status', 'meeting', 'late', 'cant_find', 'cancel', 'human']))]
  const contact = status && (status.supportEmail || status.supportPhone)

  return (
    <div className="sh-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose() }}>
      <section className="sh-panel cc-panel" role="dialog" aria-modal="true" aria-label={t('cc.name')}>
        <header className="sh-head">
          <span className="sh-avatar cc-avatar" aria-hidden="true"><Icon name="sparkle" size={22} /></span>
          <div className="grow">
            <b>{status?.name || t('cc.name')}</b>
            <small>{t('cc.ai')}{status ? ` · ${status.llmEnabled ? t('cc.modeLlm') : t('cc.modeRules')}` : ''}</small>
          </div>
          {onOpenSahayak && <button type="button" className="icon-btn" onClick={onOpenSahayak} aria-label={t('cc.sahayak')} title={t('cc.sahayak')}><Icon name="sun" /></button>}
          <button type="button" className="icon-btn" onClick={clear} aria-label={t('cc.clear')} title={t('cc.clear')}><Icon name="trash" /></button>
          <button type="button" className="icon-btn" onClick={onClose} aria-label={t('cc.close')}><Icon name="x" /></button>
        </header>

        <div className="sh-body" role="log" aria-live="polite" aria-relevant="additions">
          <div className="cc-intro">
            <p><b>{t('cc.hello', { name: firstName })}</b></p>
            <p className="muted small">{t('cc.tag')}</p>
            <p className="muted small">{t('cc.hello2')}</p>
          </div>
          {msgs.map((m) => (
            <div key={m.id} className={`sh-msg ${m.who}`}>
              <span className="sr-only">{m.who === 'me' ? t('cc.you') : t('cc.name')}: </span>
              <span>{m.text}</span>
              {m.reply?.cards?.map((c) => <Card key={c.type + c.title} card={c} />)}
              {m.reply?.actions?.length > 0 && (
                <div className="sh-tools">
                  {m.reply.actions.map((a) => (
                    <button key={a.kind + a.label} type="button" className="cc-action" onClick={() => run(a)}>{a.label}</button>
                  ))}
                </div>
              )}
              {m.reply?.verified && <small className="cc-meta"><Icon name="shield" size={13} stroke={2} /> {t('cc.verified')}</small>}
            </div>
          ))}
          {busy && <div className="sh-msg bot" role="status" aria-label={t('cc.thinking')}><span className="typing"><i /><i /><i /></span></div>}
          {failed && (
            <div className="sh-msg bot cc-error" role="alert">
              <span>{failed.message || t('cc.failed')}</span>
              <button type="button" className="cc-action" onClick={() => ask(failed.q, { repeat: true })}><Icon name="refresh" size={16} /> {t('cc.retry')}</button>
            </div>
          )}
          <div ref={end} />
        </div>

        <div className="sh-chips" role="list" aria-label={t('cc.name')}>
          {suggestions.map((s) => (
            <button key={s} role="listitem" type="button" className="sh-chip" disabled={busy} onClick={() => ask(t(`cc.sug.${s}`))}>{t(`cc.sug.${s}`)}</button>
          ))}
        </div>
        <form className="sh-input" onSubmit={(e) => { e.preventDefault(); ask(text) }}>
          <input ref={input} className="input" value={text} maxLength={800} onChange={(e) => setText(e.target.value)} placeholder={t('cc.placeholder')} aria-label={t('cc.placeholder')} />
          <button type="submit" className="btn btn-primary" disabled={busy || !text.trim()} aria-busy={busy} aria-label={t('cc.send')}><Icon name="send" size={18} /></button>
        </form>
        <p className="cc-foot muted small">
          {status ? t('cc.retention', { d: status.retentionDays }) : ''}{contact ? ` ${t('cc.contact', { c: [status.supportEmail, status.supportPhone].filter(Boolean).join(' / ') })}` : ''}
        </p>
      </section>

      {pending?.kind === 'CREATE_CASE' && (
        <CaseDialog orderId={pending.payload?.orderId} category={pending.payload?.category} summary={lastUser} source="CONCIERGE" onClose={() => setPending(null)} />
      )}
      {pending && pending.kind !== 'CREATE_CASE' && (
        <Dialog title={t('cc.confirmTitle')} onClose={() => setPending(null)} busy={sending}
          footer={(
            <>
              <button type="button" className="btn btn-primary" onClick={confirm} disabled={sending}>{sending ? t('common.sending') : pending.kind === 'SEND_MESSAGE' ? t('cc.sendAsMe') : t('cc.send2')}</button>
              <button type="button" className="btn btn-ghost" onClick={() => setPending(null)} disabled={sending}>{t('cc.notNow')}</button>
            </>
          )}>
          {pending.kind === 'SEND_MESSAGE' ? (
            <>
              <p className="muted small">{t('cc.confirmMessage')}</p>
              <textarea className="input" maxLength={500} value={draft} onChange={(e) => setDraft(e.target.value)} aria-label={t('cc.confirmMessage')} />
            </>
          ) : (
            <>
              <p><b>{pending.label}</b></p>
              <p className="muted small">{t('cc.confirmUpdate')}</p>
            </>
          )}
        </Dialog>
      )}
    </div>
  )
}
