import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/Auth'
import { useI18n } from '../i18n'
import { api } from '../lib/api'
import { answer, detectIntent } from '../lib/sahayak'
import { canListen, canSpeak, listen, speak, stopSpeaking } from '../lib/speech'
import Icon from './Icon'

const CHIPS = [
  ['where', 'pin'], ['carry', 'box'], ['time', 'clock'], ['requests', 'list'], ['online', 'sun'], ['photo', 'camera'],
]
const reduceMotion = () => typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
let seq = 0

export default function Sahayak({ onClose, onBack }) {
  const i18n = useI18n()
  const { t, speechLang } = i18n
  const { user } = useAuth()
  const [data, setData] = useState({ opp: null, me: null, stats: null })
  const [msgs, setMsgs] = useState([])
  const [typing, setTyping] = useState(false)
  const [text, setText] = useState('')
  const [listening, setListening] = useState(false)
  const [speakingId, setSpeakingId] = useState(null)
  const [auto, setAuto] = useState(() => { try { return localStorage.getItem('fw_sh_auto') === '1' } catch { return false } })
  const [voiceNote, setVoiceNote] = useState('')
  const end = useRef(null)
  const stopListen = useRef(null)
  const closeBtn = useRef(null)

  // Fresh numbers every time the panel opens.
  useEffect(() => {
    const c = new AbortController()
    Promise.allSettled([api.vendorOpportunities({}, c.signal), api.vendorMe(c.signal), api.vendorStats(c.signal)]).then(([o, m, s]) => {
      if (c.signal.aborted) return
      setData({ opp: o.value ?? null, me: m.value ?? null, stats: s.value ?? null })
    })
    return () => c.abort()
  }, [])

  useEffect(() => {
    closeBtn.current?.focus()
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    return () => { document.removeEventListener('keydown', onKey); stopSpeaking(); stopListen.current?.() }
  }, [onClose])

  useEffect(() => { end.current?.scrollIntoView({ block: 'end', behavior: reduceMotion() ? 'auto' : 'smooth' }) }, [msgs, typing])

  const say = useCallback((id, textToSay) => {
    setVoiceNote('')
    if (speakingId === id) { stopSpeaking(); setSpeakingId(null); return }
    const ok = speak(textToSay, speechLang, { onEnd: () => setSpeakingId((cur) => (cur === id ? null : cur)) })
    if (ok) setSpeakingId(id)
    else setVoiceNote(t('sh.noVoice'))
  }, [speakingId, speechLang, t])

  function ask(question, intentOverride) {
    const intent = intentOverride ?? detectIntent(question)
    setMsgs((m) => [...m, { id: ++seq, from: 'me', text: question }])
    setTyping(true)
    const ctx = { ...i18n, ...data }
    const reply = () => {
      const a = answer(intent, ctx)
      const id = ++seq
      setTyping(false)
      setMsgs((m) => [...m, { id, from: 'bot', text: a.text, actions: a.actions }])
      if (auto) setTimeout(() => say(id, a.text), 50)
    }
    if (reduceMotion()) reply(); else setTimeout(reply, 650)
  }

  function submit(e) {
    e.preventDefault()
    const q = text.trim()
    if (!q) return
    setText('')
    ask(q)
  }

  function toggleMic() {
    if (listening) { stopListen.current?.(); return }
    stopSpeaking()
    setListening(true)
    stopListen.current = listen(speechLang, {
      onResult: (q) => ask(q),
      onEnd: () => setListening(false),
      onError: () => setVoiceNote(t('sh.micError')),
    })
  }

  function toggleAuto(v) {
    setAuto(v)
    try { localStorage.setItem('fw_sh_auto', v ? '1' : '0') } catch { /* private mode */ }
  }

  return (
    <div className="sh-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section className="sh-panel" role="dialog" aria-label={t('sh.name')}>
        <header className="sh-head">
          <span className="sh-avatar"><Icon name="sparkle" size={22} /></span>
          <div className="grow">
            <b>{t('sh.name')}</b>
            <small>{t('sh.tag')}</small>
          </div>
          {onBack && <button type="button" className="icon-btn" onClick={onBack} aria-label={t('cc.name')} title={t('cc.name')}><Icon name="sparkle" /></button>}
          <button type="button" className="icon-btn" onClick={onClose} ref={closeBtn} aria-label={t('common.close')}><Icon name="x" /></button>
        </header>

        <div className="sh-body" aria-live="polite">
          <div className="sh-msg bot"><p>{t('sh.hello', { name: user?.name?.split(' ')[0] ?? '' })}</p><p>{t('sh.hello2')}</p></div>
          {msgs.map((m) => (
            <div key={m.id} className={`sh-msg ${m.from}`}>
              <p>{m.text}</p>
              {m.from === 'bot' && (
                <div className="sh-tools">
                  {canSpeak && (
                    <button type="button" className="sh-listen" onClick={() => say(m.id, m.text)} aria-pressed={speakingId === m.id}>
                      <Icon name={speakingId === m.id ? 'stop' : 'speaker'} size={18} /> {speakingId === m.id ? t('common.stop') : t('common.listen')}
                    </button>
                  )}
                  {m.actions?.map((a) => (a.to
                    ? <Link key={a.label} className="btn btn-primary btn-sm" to={a.to} onClick={onClose}>{a.label}</Link>
                    : <a key={a.label} className="btn btn-saffron btn-sm" href={a.href} target="_blank" rel="noopener noreferrer"><Icon name="nav" size={16} /> {a.label}</a>))}
                </div>
              )}
            </div>
          ))}
          {typing && <div className="sh-msg bot typing" aria-label={t('sh.typing')}><i /><i /><i /></div>}
          {voiceNote && <p className="note small">{voiceNote}</p>}
          <div ref={end} />
        </div>

        <div className="sh-chips">
          {CHIPS.map(([intent, icon]) => (
            <button key={intent} type="button" className="sh-chip" onClick={() => ask(t(`sh.q.${intent}`), intent)}>
              <Icon name={icon} size={18} /> {t(`sh.q.${intent}`)}
            </button>
          ))}
        </div>

        <form className="sh-input" onSubmit={submit}>
          {canListen && (
            <button type="button" className={`sh-mic ${listening ? 'on' : ''}`} onClick={toggleMic} aria-pressed={listening} aria-label={listening ? t('sh.listening') : t('sh.speak')}>
              <Icon name="mic" size={22} />
            </button>
          )}
          <label className="sr-only" htmlFor="sh-text">{t('sh.placeholder')}</label>
          <input id="sh-text" className="input" value={text} onChange={(e) => setText(e.target.value)} placeholder={listening ? t('sh.listening') : t('sh.placeholder')} autoComplete="off" />
          <button className="btn btn-primary" disabled={!text.trim()} aria-label={t('common.send')}><Icon name="send" size={18} /></button>
        </form>
        <label className="sh-auto check small">
          <input type="checkbox" checked={auto} onChange={(e) => toggleAuto(e.target.checked)} /> {t('sh.auto')}
        </label>
      </section>
    </div>
  )
}
