import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../i18n'
import Icon from './Icon'

// Same shape of rule the real engine uses (a request weighs 2.5; score = 100 * (1 - e^(-weight/10))),
// but only for this demo. The real engine also uses time decay, purchases, and more.
const REQUEST_WEIGHT = 2.5
const score = (n) => Math.round(100 * (1 - Math.exp(-(n * REQUEST_WEIGHT) / 10)))
const PRODUCTS = ['tomato', 'potato', 'banana']
const HOUSES = 6
const CART_X = [16.6, 50, 83.4]
const DEMO = [[1, 'tomato'], [1, 'tomato'], [1, 'potato'], [0, 'banana'], [1, 'tomato'], [2, 'potato'], [1, 'banana'], [1, 'tomato']]

function CartGlyph() {
  return (
    <svg viewBox="-34 -30 80 62" width="64" height="50" aria-hidden="true" className="cart-svg">
      <ellipse cx="0" cy="22" rx="30" ry="4" className="shadow" />
      <path d="M-24 -20h48l-5 12h-38z" className="awning" />
      <path d="M-24 -20h12l-2 12h-8zM0 -20h12l-1 12H-1zM-12 -20h12l-1 12h-10zM12 -20h12l-3 12h-9z" className="awning-stripe" />
      <rect x="-22" y="-8" width="44" height="4" rx="2" className="wood" />
      <path d="M-22 -4h44l-4 18h-36z" className="crate" />
      <circle cx="-10" cy="-1" r="5" className="tomato" />
      <circle cx="2" cy="-2" r="5" className="potato" />
      <circle cx="13" cy="-1" r="4.6" className="onion" />
      <circle cx="-14" cy="19" r="5" className="wheel" />
      <circle cx="14" cy="19" r="5" className="wheel" />
    </svg>
  )
}

function HouseIcon() {
  return (
    <svg viewBox="0 0 24 24" width="26" height="26" aria-hidden="true" className="sim-house-svg">
      <path d="M3 12 12 4l9 8" className="roof" />
      <path d="M5.5 11v8.5h13V11" className="walls" />
      <rect x="10" y="14" width="4" height="5.5" rx="1" className="door" />
    </svg>
  )
}

export default function DemandSimulator() {
  const { t, pname } = useI18n()
  const [product, setProduct] = useState('tomato')
  const [asks, setAsks] = useState({}) // "area-house" -> product
  const [demoing, setDemoing] = useState(false)
  const [moving, setMoving] = useState(false)
  const timers = useRef([])
  const reduce = useRef(typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches)

  const areas = [0, 1, 2].map((a) => {
    const mine = Object.entries(asks).filter(([k]) => k.startsWith(`${a}-`)).map(([, p]) => p)
    const byProduct = {}
    mine.forEach((p) => { byProduct[p] = (byProduct[p] || 0) + 1 })
    return { a, n: mine.length, score: score(mine.length), top: Object.entries(byProduct).sort((x, y) => y[1] - x[1]).map(([p]) => p) }
  })
  const best = areas.reduce((m, x) => (x.score > m.score ? x : m), { score: 0, a: -1 })
  const bestArea = best.score >= 20 ? best : null
  const level = !bestArea ? null : bestArea.score >= 65 ? 'HIGH' : bestArea.score >= 35 ? 'MEDIUM' : 'LOW'
  const cartPos = bestArea ? CART_X[bestArea.a] : 8

  useEffect(() => {
    if (!bestArea) return undefined
    const start = setTimeout(() => setMoving(true), 0)
    const id = setTimeout(() => setMoving(false), 1500)
    return () => { clearTimeout(start); clearTimeout(id) }
  }, [bestArea?.a]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => () => timers.current.forEach(clearTimeout), [])

  function stopDemo() {
    timers.current.forEach(clearTimeout)
    timers.current = []
    setDemoing(false)
  }

  function toggle(a, h) {
    stopDemo()
    const key = `${a}-${h}`
    setAsks((cur) => {
      const next = { ...cur }
      if (next[key] === product) delete next[key]
      else next[key] = product
      return next
    })
  }

  function reset() {
    stopDemo()
    setAsks({})
  }

  function demo() {
    stopDemo()
    setAsks({})
    if (reduce.current) {
      const all = {}
      let h = [0, 0, 0]
      DEMO.forEach(([a, p]) => { all[`${a}-${h[a]++}`] = p })
      setAsks(all)
      return
    }
    setDemoing(true)
    const used = [0, 0, 0]
    DEMO.forEach(([a, p], i) => {
      timers.current.push(setTimeout(() => {
        setAsks((cur) => ({ ...cur, [`${a}-${used[a]++}`]: p }))
        if (i === DEMO.length - 1) setDemoing(false)
      }, 500 + i * 650))
    })
  }

  const areaName = (a) => t(`scene.a${a + 1}`)

  return (
    <div className="card sim">
      <div className="sim-top">
        <div className="chips" role="group" aria-label={t('sim.pick')}>
          {PRODUCTS.map((p) => (
            <button key={p} type="button" className={`chip-btn ${product === p ? 'on' : ''}`} aria-pressed={product === p} onClick={() => setProduct(p)}>
              {pname(p.charAt(0).toUpperCase() + p.slice(1))}
            </button>
          ))}
        </div>
        <div className="row-gap">
          <button type="button" className="btn btn-saffron btn-sm" onClick={demo} disabled={demoing}><Icon name="sparkle" size={16} /> {t('sim.demo')}</button>
          <button type="button" className="btn btn-ghost btn-sm" onClick={reset}><Icon name="refresh" size={16} /> {t('sim.reset')}</button>
        </div>
      </div>
      <p className="sim-hint">{t('sim.hint', { p: pname(product.charAt(0).toUpperCase() + product.slice(1)) })}</p>

      <div className="sim-areas">
        {areas.map(({ a, n, score: sc }) => (
          <div key={a} className={`sim-area ${bestArea?.a === a ? 'best' : ''}`} style={{ '--heat': Math.min(1, sc / 100) }}>
            <div className="sim-area-head">
              <b>{areaName(a)}</b>
              <span className="sim-score">{sc}</span>
            </div>
            <div className="sim-houses">
              {Array.from({ length: HOUSES }, (_, h) => {
                const ask = asks[`${a}-${h}`]
                return (
                  <button key={h} type="button" className={`sim-house ${ask ? 'asking' : ''}`} aria-pressed={!!ask}
                    aria-label={t('sim.house', { area: areaName(a), n: h + 1, state: ask ? pname(ask.charAt(0).toUpperCase() + ask.slice(1)) : t('sim.nothing') })}
                    onClick={() => toggle(a, h)}>
                    <HouseIcon />
                    {ask && <span className="sim-bubble" key={ask}>{pname(ask.charAt(0).toUpperCase() + ask.slice(1)).slice(0, 6)}</span>}
                  </button>
                )
              })}
            </div>
            <div className="sim-bar" aria-hidden="true"><i style={{ width: `${sc}%` }} /></div>
            <small className="muted">{t('sim.asking', { n })}</small>
          </div>
        ))}
      </div>

      <div className="sim-road" aria-hidden="true">
        <div className={`sim-cart ${moving ? 'moving' : ''}`} style={{ left: `${cartPos}%` }}><CartGlyph /></div>
      </div>

      <div className="sim-result" role="status" aria-live="polite">
        {bestArea ? (
          <>
            <span className={`level ${level === 'HIGH' ? 'lv-high' : level === 'MEDIUM' ? 'lv-med' : 'lv-low'}`}>{t(`level.${level}`)}</span>
            <p><b>{areaName(bestArea.a)}</b> — {t('sim.result', { n: bestArea.n, list: bestArea.top.map((p) => pname(p.charAt(0).toUpperCase() + p.slice(1))).join(', ') })}</p>
          </>
        ) : (
          <p className="muted">{t('sim.empty')}</p>
        )}
        <small className="muted">{t('sim.note')}</small>
      </div>
    </div>
  )
}
