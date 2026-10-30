import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/Auth'
import { useToast } from '../context/Toast'
import { api } from '../lib/api'
import { useLive } from '../lib/useLive'
import Icon from './Icon'
import { useI18n } from '../i18n'

export default function Notifications() {
  const { user } = useAuth()
  const toast = useToast()
  const { t, ago } = useI18n()
  const nav = useNavigate()
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])
  const [unread, setUnread] = useState(0)
  const [failed, setFailed] = useState(false)
  const box = useRef(null)

  const load = useCallback((signal) => {
    api.notifications(signal)
      .then((r) => { setItems(r.items); setUnread(r.unread); setFailed(false) })
      .catch((e) => { if (e.code !== 'ABORTED') setFailed(true) })
  }, [])

  useEffect(() => {
    if (!user) return undefined
    const c = new AbortController()
    load(c.signal)
    return () => c.abort()
  }, [user, load])

  useLive(user ? '/user/queue/notifications' : null, (n) => {
    setItems((list) => [n, ...list.filter((x) => x.id !== n.id)].slice(0, 50))
    setUnread((u) => u + 1)
    toast.info(n.title)
    try { navigator.vibrate?.(60) } catch { /* not supported */ }
  })

  useEffect(() => {
    if (!open) return undefined
    const onDown = (e) => { if (box.current && !box.current.contains(e.target)) setOpen(false) }
    const onKey = (e) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => { document.removeEventListener('mousedown', onDown); document.removeEventListener('keydown', onKey) }
  }, [open])

  if (!user) return null

  async function toggle() {
    const next = !open
    setOpen(next)
    if (next && unread > 0) {
      try { await api.readAllNotifications(); setUnread(0); setItems((l) => l.map((x) => ({ ...x, read: true }))) } catch { /* keep badge */ }
    }
  }

  return (
    <div className="bell" ref={box}>
      <button type="button" className="icon-btn" onClick={toggle} aria-label={unread ? t('notif.new', { n: unread }) : t('notif.title')} aria-expanded={open}>
        <Icon name="bell" />
        {unread > 0 && <span className="badge">{unread > 9 ? '9+' : unread}</span>}
      </button>
      {open && (
        <div className="sheet-pop" role="dialog" aria-label={t('notif.title')}>
          <h3>{t('notif.title')}</h3>
          {failed && <p className="muted small">{t('notif.failed')}</p>}
          {!failed && items.length === 0 && <p className="muted">{t('notif.empty')}</p>}
          <ul>
            {items.map((n) => (
              <li key={n.id}>
                <button type="button" className={n.read ? '' : 'unread'} onClick={() => { setOpen(false); if (n.link) nav(n.link) }}>
                  <b>{n.title}</b>
                  {n.body && <span>{n.body}</span>}
                  <small>{ago(n.createdAt)}</small>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
