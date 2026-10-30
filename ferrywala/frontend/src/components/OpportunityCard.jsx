import { useState } from 'react'
import { useI18n } from '../i18n'
import { buildReasons, buildSummary } from '../lib/advice'
import { estimateRange, navigateUrl } from '../lib/format'
import { canSpeak, speak, stopSpeaking } from '../lib/speech'
import Icon from './Icon'

const LEVEL_CLS = { HIGH: 'lv-high', MEDIUM: 'lv-med', LOW: 'lv-low' }

/** "Today's best opportunity". One decision, with the reasons one tap away, and a Listen button. */
export default function OpportunityCard({ o, featured = false }) {
  const i18n = useI18n()
  const { t, pname, windowLabel, dist } = i18n
  const [why, setWhy] = useState(false)
  const [speaking, setSpeaking] = useState(false)
  const [noVoice, setNoVoice] = useState(false)

  function listen() {
    if (speaking) { stopSpeaking(); setSpeaking(false); return }
    const ok = speak(buildSummary(o, i18n), i18n.speechLang, { onEnd: () => setSpeaking(false) })
    setSpeaking(ok)
    setNoVoice(!ok)
  }

  return (
    <section className={`card opp ${featured ? 'opp-featured' : ''}`} aria-label={featured ? t('opp.best') : `${t('opp.area')}: ${o.areaName}`}>
      {featured && <p className="eyebrow opp-eyebrow"><span className="flame-pulse"><Icon name="flame" size={14} /></span> {t('opp.best')}</p>}
      <div className="spread">
        <h2 className="opp-area">{o.areaName}</h2>
        <span className={`level ${LEVEL_CLS[o.level] ?? 'lv-low'}`}>{t(`level.${o.level}`)}</span>
      </div>

      <dl className="opp-grid">
        <div><dt>{t('opp.customers')}</dt><dd>{estimateRange(o.estimatedCustomersMin, o.estimatedCustomersMax)}</dd></div>
        <div><dt>{t('opp.time')}</dt><dd>{windowLabel(o.recommendedWindow)}{!o.recommendedWindow.fromData && <small> ({t('opp.typical')})</small>}</dd></div>
        <div><dt>{t('opp.distance')}</dt><dd>{o.distanceKm != null ? dist(o.distanceKm) : '—'}</dd></div>
        <div><dt>{t('opp.data')}</dt><dd>{t(`conf.${o.confidence}`)}</dd></div>
      </dl>

      {o.topProducts.length > 0 && (
        <div>
          <p className="small muted">{t('opp.topProducts')}</p>
          <div className="chips opp-products">
            {o.topProducts.map((p) => (
              <span key={p.name} className={`chip ${p.vendorCarries ? 'chip-green' : 'chip-terra'}`} title={p.vendorCarries ? t('opp.youCarry') : t('opp.notCarried')}>
                {pname(p.name)}{!p.vendorCarries && ` · ${t('opp.addToCarry')}`}
              </span>
            ))}
          </div>
        </div>
      )}

      <div className="row-gap">
        <a className="btn btn-saffron btn-lg opp-go" href={navigateUrl(o.latitude, o.longitude)} target="_blank" rel="noopener noreferrer">
          <Icon name="nav" size={20} /> {t('opp.go')}
        </a>
        {canSpeak && (
          <button type="button" className="btn btn-ghost" onClick={listen} aria-pressed={speaking}>
            <Icon name={speaking ? 'stop' : 'speaker'} size={18} /> {speaking ? t('common.stop') : t('common.listen')}
          </button>
        )}
        <button type="button" className="btn btn-ghost" onClick={() => setWhy((w) => !w)} aria-expanded={why}>{t('opp.why')}</button>
      </div>
      <p className="small muted opp-disclaimer">{t('opp.disclaimer')}</p>
      {noVoice && <p className="note small">{t('sh.noVoice')}</p>}

      {why && (
        <ul className="ticks why">
          {buildReasons(o, i18n).map((r) => <li key={r}>{r}</li>)}
        </ul>
      )}
    </section>
  )
}
