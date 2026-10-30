import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useI18n } from '../i18n'
import Icon from './Icon'

const FLAG = 'fw_onboard_done'
const seen = () => { try { return localStorage.getItem(FLAG) === '1' } catch { return false } }

const CONFETTI = Array.from({ length: 16 }, (_, i) => i)

/** Four small steps with a progress ring. Finishing the last one gets a short celebration. */
export default function Onboarding({ me, productCount, onGoOnline }) {
  const { t } = useI18n()
  const [dismissed, setDismissed] = useState(seen)
  const steps = [
    { key: 'photo', done: !!me.imageUrl, to: '/vendor/profile', icon: 'camera' },
    { key: 'location', done: Number.isFinite(me.latitude), to: '/vendor/profile', icon: 'pin' },
    { key: 'products', done: productCount > 0, to: '/vendor/products', icon: 'tag' },
    { key: 'online', done: me.online, action: onGoOnline, icon: 'sun' },
  ]
  const doneCount = steps.filter((s) => s.done).length
  const all = doneCount === steps.length
  const next = steps.find((s) => !s.done)

  function close() {
    setDismissed(true)
    try { localStorage.setItem(FLAG, '1') } catch { /* private mode */ }
  }

  if (dismissed) return null
  const R = 26
  const C = 2 * Math.PI * R

  if (all) {
    return (
      <section className="card celebrate" role="status">
        <div className="confetti" aria-hidden="true">{CONFETTI.map((i) => <i key={i} style={{ '--i': i }} />)}</div>
        <h2>{t('onb.doneTitle')}</h2>
        <p className="muted">{t('onb.doneSub')}</p>
        <button type="button" className="btn btn-primary" onClick={close}>{t('onb.great')}</button>
      </section>
    )
  }

  return (
    <section className="card onboarding" aria-label={t('onb.title')}>
      <div className="row">
        <svg className="ring" width="64" height="64" viewBox="0 0 64 64" aria-hidden="true">
          <circle cx="32" cy="32" r={R} className="ring-bg" />
          <circle cx="32" cy="32" r={R} className="ring-fg" strokeDasharray={C} strokeDashoffset={C * (1 - doneCount / steps.length)} />
          <text x="32" y="37" textAnchor="middle">{doneCount}/{steps.length}</text>
        </svg>
        <div className="grow">
          <h3>{t('onb.title')}</h3>
          <p className="muted small">{t('onb.sub')}</p>
        </div>
      </div>
      <ol className="onb-steps">
        {steps.map((s) => {
          const inner = (
            <>
              <span className={`onb-mark ${s.done ? 'done' : ''}`}>{s.done ? <Icon name="check" size={16} stroke={2.6} /> : <Icon name={s.icon} size={16} />}</span>
              <span className="grow">{t(`onb.${s.key}`)}</span>
              {!s.done && <Icon name="chevron" size={16} />}
            </>
          )
          const cls = `onb-step ${s.done ? 'done' : ''} ${s === next ? 'next' : ''}`
          return (
            <li key={s.key}>
              {s.done ? <div className={cls}>{inner}</div>
                : s.to ? <Link className={cls} to={s.to}>{inner}</Link>
                  : <button type="button" className={cls} onClick={s.action}>{inner}</button>}
            </li>
          )
        })}
      </ol>
    </section>
  )
}
