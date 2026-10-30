import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import CaseDialog from '../components/CaseDialog'
import Dialog from '../components/Dialog'
import Icon from '../components/Icon'
import LazyMap from '../components/LazyMap'
import { CardSkeleton, ErrorState } from '../components/States'
import { useToast } from '../context/Toast'
import { useI18n } from '../i18n'
import { api } from '../lib/api'
import { openConcierge } from '../lib/concierge'
import { navigateUrl, timeAgo } from '../lib/format'
import { useAsync } from '../lib/useAsync'
import { useTitle } from '../lib/useTitle'

const DURATIONS = [15, 30, 60]
const PUSH_MS = 15000
const POLL_MS = 15000
const fmtClock = (iso) => new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
const dist = (m) => (m < 1000 ? `${Math.round(m / 10) * 10} m` : `${(m / 1000).toFixed(1)} km`)

/**
 * Meeting page for one accepted request. Sharing a location is optional, explicit, time-limited and
 * revocable; everything else (updates, messages, directions to the pinned spot) works without it.
 */
export default function Meet() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const nav = useNavigate()
  const { t, errMsg } = useI18n()
  const toast = useToast()
  useTitle(t('meet.title'))

  const meet = useAsync((s) => api.meeting(id, s), [id])
  const { reload } = meet
  const m = meet.data

  const [eta, setEta] = useState(10)
  const [late, setLate] = useState(false)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [duration, setDuration] = useState(15)
  const [askShare, setAskShare] = useState(params.get('share') === '1')
  const [sharing, setSharing] = useState(false)
  const [kase, setKase] = useState(false)
  const watch = useRef(null)
  const lastPos = useRef(null)
  const timer = useRef(null)

  // Keep the page fresh without a manual reload, but only while it is visible.
  useEffect(() => {
    const tick = () => { if (document.visibilityState === 'visible') reload() }
    const h = setInterval(tick, POLL_MS)
    return () => clearInterval(h)
  }, [reload])

  const stopLocal = useCallback(() => {
    if (watch.current != null) navigator.geolocation?.clearWatch(watch.current)
    clearInterval(timer.current)
    watch.current = null
    timer.current = null
    lastPos.current = null
    setSharing(false)
  }, [])
  useEffect(() => stopLocal, [stopLocal])

  const myActive = !!m?.myShare?.active
  // If the server says my share ended (expiry or stop elsewhere), stop sending positions.
  useEffect(() => { if (m && !myActive && watch.current != null) stopLocal() }, [m, myActive, stopLocal])

  async function begin() {
    setAskShare(false)
    if (!navigator.geolocation) { toast.error(t('meet.share.unsupported')); return }
    try {
      const first = await new Promise((ok, no) => navigator.geolocation.getCurrentPosition(ok, no, { enableHighAccuracy: true, timeout: 12000 }))
      lastPos.current = first.coords
      await api.startShare(id, { latitude: first.coords.latitude, longitude: first.coords.longitude, durationMinutes: duration })
      setSharing(true)
      watch.current = navigator.geolocation.watchPosition((p) => { lastPos.current = p.coords }, () => {}, { enableHighAccuracy: true, maximumAge: 10000 })
      timer.current = setInterval(() => {
        const c = lastPos.current
        if (!c) return
        api.startShare(id, { latitude: c.latitude, longitude: c.longitude, durationMinutes: duration })
          .then(() => reload())
          .catch(() => stopLocal())
      }, PUSH_MS)
      reload()
    } catch (e) {
      if (e?.code === 1) toast.error(t('meet.share.denied'))
      else if (e?.status) toast.error(errMsg(e))
      else toast.error(t('meet.share.failed'))
    }
  }

  async function stop() {
    stopLocal()
    try { await api.stopShare(id) } catch (e) { toast.error(errMsg(e)) }
    reload()
  }

  async function update(type, etaMinutes) {
    setBusy(true)
    try {
      await api.meetingUpdate(id, { type, etaMinutes, source: 'APP' })
      toast.success(t('cc.sentOk'))
      setLate(false)
      reload()
    } catch (e) { toast.error(errMsg(e)) } finally { setBusy(false) }
  }

  async function send(e) {
    e.preventDefault()
    if (!text.trim()) return
    setBusy(true)
    try { await api.sendMessage(id, text.trim()); setText(''); reload() } catch (err) { toast.error(errMsg(err)) } finally { setBusy(false) }
  }

  if (meet.loading && !m) return <div className="page narrow"><CardSkeleton rows={4} /></div>
  if (meet.error && !m) return <div className="page narrow"><ErrorState title={meet.error.status === 404 ? t('meet.notAvailable') : undefined} error={meet.error.status === 404 ? null : meet.error} onRetry={reload} /></div>

  const other = m.otherName
  const markers = [{ id: 'spot', lat: m.latitude, lng: m.longitude, kind: 'demand', label: '★', title: t('meet.point'), body: m.areaName || undefined }]
  const lines = []
  if (m.otherShare?.active && m.otherShare.latitude != null) {
    markers.push({ id: 'other', lat: m.otherShare.latitude, lng: m.otherShare.longitude, kind: 'vendor', title: t('meet.share.theirPin'), body: other })
    lines.push({ id: 'to-spot', points: [[m.otherShare.latitude, m.otherShare.longitude], [m.latitude, m.longitude]] })
  }
  const pointText = m.areaName ? t('meet.pointText', { area: m.areaName }) : t('meet.pointText', { area: t('meet.pointUnnamed') })
  const label = (u) => (u.type === 'RUNNING_LATE' || u.type === 'PROPOSE_TIME'
    ? t(`meet.u.${u.type}`, { who: u.mine ? t('cc.you') : u.senderName, n: u.etaMinutes ?? '?' })
    : t(`meet.u.${u.type}`, { who: u.mine ? t('cc.you') : u.senderName }))

  return (
    <div className="page narrow stack meet">
      <header className="page-head">
        <button type="button" className="btn btn-ghost btn-sm" onClick={() => nav(-1)}><Icon name="chevron" size={16} /> {t('meet.back')}</button>
        <h1>{t('meet.with', { name: other })}</h1>
        <p className="muted">{t('meet.request', { id: m.orderId })}{m.shopName ? ` · ${m.shopName}` : ''}</p>
      </header>

      {!m.coordinationOpen && <p className="note" role="status">{m.messagingOpen ? t('meet.closedPending') : t('meet.closedEnded')}</p>}

      <section className="card meet-point reveal" aria-labelledby="mp">
        <h2 id="mp">{t('meet.point')}</h2>
        <p>{pointText}</p>
        {m.note && <p className="muted small">{m.note}</p>}
        <LazyMap markers={markers} lines={lines} height={260} label={t('meet.mapLabel')} />
        {m.distanceMeters != null && <p className="small" role="status"><Icon name="nav" size={16} /> {t('meet.share.dist', { d: dist(m.distanceMeters) })}</p>}
        <a className="btn btn-saffron btn-sm" href={navigateUrl(m.latitude, m.longitude)} target="_blank" rel="noopener noreferrer"><Icon name="nav" size={16} /> {t('meet.directions')}</a>
        <p className="muted small">{t('meet.directionsNote')}</p>
      </section>

      {m.coordinationOpen && (
        <>
          <section className="card reveal" aria-labelledby="mu">
            <h2 id="mu">{t('meet.sendTo', { name: other })}</h2>
            <div className="meet-btns">
              <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => setLate((v) => !v)} aria-expanded={late}>{t('meet.btn.late')}</button>
              <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => update('ARRIVED')}>{t('meet.btn.arrived')}</button>
              <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => update('CANT_FIND')}>{t('meet.btn.cantFind')}</button>
              <button type="button" className="btn btn-ghost" disabled={busy} onClick={() => update('NEED_HELP')}>{t('meet.btn.needHelp')}</button>
            </div>
            {late && (
              <div className="meet-eta">
                <label htmlFor="eta">{t('meet.eta')}</label>
                <select id="eta" className="input" value={eta} onChange={(e) => setEta(Number(e.target.value))}>
                  {[5, 10, 15, 20, 30, 45, 60].map((n) => <option key={n} value={n}>{t('meet.minutes', { n })}</option>)}
                </select>
                <button type="button" className="btn btn-primary" disabled={busy} onClick={() => update('RUNNING_LATE', eta)}>{t('meet.sendUpdate')}</button>
              </div>
            )}
          </section>

          <section className="card reveal" aria-labelledby="ms">
            <h2 id="ms">{t('meet.share.title')}</h2>
            <p className="muted small">{t('meet.share.explain', { name: other })}</p>
            {myActive
              ? <p role="status"><Icon name="shield" size={16} /> {t('meet.share.active', { t: fmtClock(m.myShare.expiresAt) })}</p>
              : <p className="muted small">{t('meet.share.off')}</p>}
            {m.otherShare?.active
              ? <p className="small" role="status">{t('meet.share.other', { name: other })}{m.otherShare.updatedAt ? ` · ${timeAgo(m.otherShare.updatedAt)}` : ''}</p>
              : <p className="muted small">{t('meet.share.otherOff', { name: other })}</p>}
            {myActive || sharing
              ? <button type="button" className="btn btn-danger" onClick={stop}>{t('meet.share.stop')}</button>
              : (
                <div className="meet-eta">
                  <label htmlFor="dur">{t('meet.share.duration')}</label>
                  <select id="dur" className="input" value={duration} onChange={(e) => setDuration(Number(e.target.value))}>
                    {DURATIONS.map((n) => <option key={n} value={n}>{t('meet.minutes', { n })}</option>)}
                  </select>
                  <button type="button" className="btn btn-primary" onClick={() => setAskShare(true)}>{t('meet.share.start')}</button>
                </div>
              )}
          </section>
        </>
      )}

      <section className="card reveal" aria-labelledby="mh">
        <h2 id="mh">{t('meet.updates')}</h2>
        {m.updates.length === 0
          ? <p className="muted">{t('meet.noUpdates')}</p>
          : <ul className="meet-list">{m.updates.map((u) => (
            <li key={u.id} className={u.mine ? 'mine' : ''}>{label(u)}{u.viaConcierge ? <small> · {t('meet.viaAi')}</small> : null}<small className="muted"> · {timeAgo(u.createdAt)}</small></li>
          ))}</ul>}
      </section>

      <section className="card reveal" aria-labelledby="mm">
        <h2 id="mm">{t('meet.chat')}</h2>
        {m.messages.length === 0
          ? <p className="muted">{t('meet.noMessages')}</p>
          : <ul className="meet-list chat">{m.messages.map((x) => (
            <li key={x.id} className={x.mine ? 'mine' : ''}><b>{x.mine ? t('cc.you') : x.senderName}</b> {x.body}<small className="muted"> · {timeAgo(x.createdAt)}</small></li>
          ))}</ul>}
        {m.messagingOpen && (
          <form className="sh-input" onSubmit={send}>
            <input className="input" value={text} maxLength={500} onChange={(e) => setText(e.target.value)} placeholder={t('meet.chatPlaceholder')} aria-label={t('meet.chatPlaceholder')} />
            <button className="btn btn-primary" disabled={busy || !text.trim()} aria-label={t('cc.send')}><Icon name="send" size={18} /></button>
          </form>
        )}
      </section>

      <div className="meet-foot">
        <button type="button" className="btn btn-ghost" onClick={() => openConcierge(m.orderId)}><Icon name="sparkle" size={16} /> {t('meet.ask')}</button>
        <button type="button" className="btn btn-ghost" onClick={() => setKase(true)}>{t('meet.problem')}</button>
        <Link className="btn btn-ghost" to="/support">{t('support.title')}</Link>
      </div>

      {askShare && (
        <Dialog title={t('meet.share.confirmTitle', { name: other })} onClose={() => setAskShare(false)}
          footer={(
            <>
              <button type="button" className="btn btn-primary" onClick={begin}>{t('meet.share.start')}</button>
              <button type="button" className="btn btn-ghost" onClick={() => setAskShare(false)}>{t('cc.notNow')}</button>
            </>
          )}>
          <p>{t('meet.share.confirmBody', { name: other, m: duration })}</p>
        </Dialog>
      )}
      {kase && <CaseDialog orderId={m.orderId} onClose={() => setKase(false)} />}
    </div>
  )
}
