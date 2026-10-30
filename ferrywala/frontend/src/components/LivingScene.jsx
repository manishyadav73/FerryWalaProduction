import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../i18n'

/*
 * The FerryWala story as a calm, looping illustration:
 *   1 customers ask  ->  2 needs add up  ->  3 best area lights up  ->  4 vendor heads there
 *
 * Purely illustrative (labelled as such). Pure SVG + CSS transitions: no libraries,
 * no images, nothing to download. Pauses off-screen / in background tabs, and with
 * prefers-reduced-motion it shows the finished picture with no movement at all.
 */


const ROUTE = 'M110 372 C 210 372, 300 330, 380 280 S 520 214, 566 196'
const ROUTE_REL = 'M0 0 C 100 0, 190 -42, 270 -92 S 410 -158, 456 -176'
const START = { x: 110, y: 372 }
const END = { x: 566, y: 196 }

// stage durations (ms): 0 idle, 1 ask, 2 gather, 3 light-up, 4 drive, 5 hold
const DURATIONS = [900, 2300, 2100, 1900, 3400, 2200]

const HOUSES = {
  a: [[170, 150], [205, 132], [240, 156], [190, 182], [228, 190]],
  b: [[520, 128], [556, 112], [594, 132], [532, 160], [572, 168], [612, 158]],
  c: [[770, 190], [806, 172], [842, 196], [790, 224], [830, 232]],
}

/** Phones that are short on CPU/memory, or have data-saver on, get the finished picture without motion. */
function lowPower() {
  if (typeof navigator === 'undefined') return false
  const n = navigator
  return !!(n.connection?.saveData) || (n.deviceMemory && n.deviceMemory <= 2) || (n.hardwareConcurrency && n.hardwareConcurrency <= 2)
}

// where the floating "needed" chips start; lines run from here to the winning area
const AREA = { x: 566, y: 152 }

const CHIPS = [
  { p: 'tomato', x: 470, y: 78, dx: 78, dy: 62, d: 0 },
  { p: 'potato', x: 604, y: 70, dx: -40, dy: 70, d: 140 },
  { p: 'onion', x: 540, y: 52, dx: 22, dy: 88, d: 280 },
  { p: 'tomato', x: 150, y: 96, dx: 0, dy: 0, d: 420, quiet: true },
  { p: 'spinach', x: 750, y: 140, dx: 0, dy: 0, d: 560, quiet: true },
]

// NEW: quiet location pins on road crossings, and floating marketplace cards (names come from pname)
const PINS = [{ x: 300, y: 120 }, { x: 700, y: 120 }, { x: 700, y: 300 }]
const FLOATS = [
  { p: 'Tomato', x: 250, y: 44, color: '#D8402A', d: 1600, b: 0 },
  { p: 'Potato', x: 790, y: 88, color: '#C9A26B', d: 1850, b: -2000 },
  { p: 'Onion', x: 790, y: 322, color: '#B05A8A', d: 2100, b: -4000 },
]

function House({ x, y, lit }) {
  return (
    <g transform={`translate(${x} ${y})`} className={`house ${lit ? 'lit' : ''}`}>
      <path d="M-11 5 0 -7 11 5V14H-11Z" />
      <rect x="-3" y="6" width="6" height="8" rx="1" className="door" />
    </g>
  )
}

// NEW: a map pin whose tip sits on (x, y); the ring pulses outward from the tip
function Pin({ x, y }) {
  return (
    <g transform={`translate(${x} ${y})`} className="sc-pin quiet">
      <ellipse className="ring" cx="0" cy="0" rx="10" ry="4" />
      <path d="M0 0C-7-9-11-14-11-20a11 11 0 0 1 22 0C11-14 7-9 0 0Z" />
      <circle className="hole" cx="0" cy="-20" r="4" />
    </g>
  )
}

// NEW: small floating product card (outer group fades in, inner group bobs)
function FloatCard({ x, y, label, color, d, b }) {
  const w = label.length * 7.2 + 46
  return (
    <g transform={`translate(${x} ${y})`}>
      <g className="sc-float" style={{ '--d': `${d}ms` }}>
        <g className="sc-float-bob" style={{ '--b': `${b}ms` }}>
          <rect className="fc-bg" x={-w / 2} y="-16" width={w} height="32" rx="16" />
          <circle cx={-w / 2 + 17} cy="0" r="7" fill={color} />
          <text x={-w / 2 + 30} y="4.5">{label}</text>
        </g>
      </g>
    </g>
  )
}

function Cart({ moving = false }) {
  return (
    <g className={`cart-svg ${moving ? 'moving' : ''}`} transform="scale(1.3)">
      <ellipse cx="0" cy="22" rx="30" ry="4" className="shadow" />
      <path d="M-24 -20h48l-5 12h-38z" className="awning" />
      <path d="M-24 -20h12l-2 12h-8zM0 -20h12l-1 12H-1zM-12 -20h12l-1 12h-10zM12 -20h12l-3 12h-9z" className="awning-stripe" />
      <rect x="-22" y="-8" width="44" height="4" rx="2" className="wood" />
      <path d="M-22 -4h44l-4 18h-36z" className="crate" />
      <circle cx="-10" cy="-1" r="5" className="tomato" />
      <circle cx="2" cy="-2" r="5" className="potato" />
      <circle cx="13" cy="-1" r="4.6" className="onion" />
      <g className="wheel-g" transform="translate(-14 19)"><circle r="5" className="wheel" /><path d="M0 -4V4M-4 0H4" className="spoke" /></g>
      <g className="wheel-g" transform="translate(14 19)"><circle r="5" className="wheel" /><path d="M0 -4V4M-4 0H4" className="spoke" /></g>
      <path d="M22 -2h12" className="handle" />
      <g className="vendor-fig">
        <ellipse cx="48" cy="22" rx="10" ry="2.5" className="shadow" />
        <path d="M43 22V8h4v14zM49 22V8h4v14z" className="vf-legs" />
        <rect x="41" y="-10" width="14" height="20" rx="6" className="vf-shirt" />
        <path d="M42 -6 35 -2" className="vf-arm" />
        <circle cx="48" cy="-16" r="6.5" className="vf-head" />
        <path d="M41.5 -17a6.5 6.5 0 0 1 13 0z" className="vf-cap" />
      </g>
    </g>
  )
}

/** Moves the cart along the route. Started explicitly: SMIL animations added after load
 *  would otherwise begin relative to page load and appear already finished. */
function Drive() {
  const anim = useRef(null)
  useEffect(() => {
    anim.current?.beginElement?.()
  }, [])
  return (
    <g>
      <animateMotion ref={anim} begin="indefinite" dur="3.1s" fill="freeze" path={ROUTE_REL} calcMode="spline" keyTimes="0;1" keySplines="0.45 0 0.25 1" />
      <Cart moving />
    </g>
  )
}

export default function LivingScene({ compact = false }) {
  const { t, pname } = useI18n()
  const STEPS = [t('scene.s1'), t('scene.s2'), t('scene.s3'), t('scene.s4')]
  const [reduce] = useState(
    () => typeof window !== 'undefined' && (!!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches || lowPower()),
  )
  const [stage, setStage] = useState(reduce ? 5 : 0)
  const [cycle, setCycle] = useState(0)
  const [visible, setVisible] = useState(true)
  const root = useRef(null)

  useEffect(() => {
    const el = root.current
    if (!el || typeof IntersectionObserver === 'undefined') return undefined
    const io = new IntersectionObserver(([e]) => setVisible(e.isIntersecting), { threshold: 0.15 })
    io.observe(el)
    const onVis = () => setVisible((v) => (document.hidden ? false : v))
    document.addEventListener('visibilitychange', onVis)
    return () => { io.disconnect(); document.removeEventListener('visibilitychange', onVis) }
  }, [])

  useEffect(() => {
    if (reduce || !visible) return undefined
    const t = setTimeout(() => {
      if (stage >= 5) { setStage(0); setCycle((c) => c + 1) } else setStage(stage + 1)
    }, DURATIONS[stage])
    return () => clearTimeout(t)
  }, [stage, visible, reduce])

  const ask = stage >= 1
  const gather = stage >= 2
  const lit = stage >= 3
  const drive = stage >= 4
  const activeStep = Math.min(3, Math.max(0, stage - 1))

  return (
    <figure className={`scene ${compact ? 'scene-compact' : ''} ${reduce ? 'calm' : ''}`} ref={root} aria-label={t('scene.aria')}>
      <span className="scene-tag">{t('scene.demo')}</span>
      <svg viewBox="0 0 960 440" role="img" aria-hidden="true" preserveAspectRatio="xMidYMid meet">
        <defs>
          <radialGradient id="heat" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#F2A81D" stopOpacity="0.85" />
            <stop offset="60%" stopColor="#D8572A" stopOpacity="0.38" />
            <stop offset="100%" stopColor="#D8572A" stopOpacity="0" />
          </radialGradient>
        </defs>

        <rect x="8" y="8" width="944" height="424" rx="28" className="sc-map" />
        <g className="sc-roads">
          <path d="M8 300H952M8 120H952M300 8V432M700 8V432M480 8 360 432" />
        </g>
        <g className="sc-parks">
          <ellipse cx="90" cy="70" rx="70" ry="38" />
          <ellipse cx="880" cy="360" rx="64" ry="40" />
          <ellipse cx="400" cy="380" rx="48" ry="28" />
        </g>

        {/* area labels */}
        <text x="205" y="222" className="sc-area">{t('scene.a1')}</text>
        <text x="566" y="250" className={`sc-area ${lit ? 'on' : ''}`}>{t('scene.a2')}</text>
        <text x="806" y="262" className="sc-area">{t('scene.a3')}</text>

        {/* NEW: quiet location pins on the road crossings */}
        {PINS.map((p) => <Pin key={`${p.x}-${p.y}`} x={p.x} y={p.y} />)}

        {/* demand glow on the winning area */}
        <circle cx="566" cy="152" r="108" fill="url(#heat)" className={`sc-heat ${gather ? 'show' : ''}`} />
        <circle cx="566" cy="152" r="86" className={`sc-ring ${lit ? 'show' : ''}`} />

        {/* signals travelling to the area */}
        {CHIPS.filter((c) => !c.quiet).map((c) => (
          <line key={`l${c.p}${c.x}`} x1={c.x + c.dx} y1={c.y + c.dy} x2={AREA.x} y2={AREA.y - 8} className={`sc-link ${gather ? 'show' : ''}`} />
        ))}

        {Object.entries(HOUSES).map(([k, hs]) => hs.map(([x, y]) => (
          <House key={`${k}${x}`} x={x} y={y} lit={k === 'b' && ask} />
        )))}

        {/* demand pulses over the busy houses (a few times, then they settle) */}
        {HOUSES.b.map(([x, y], i) => (
          <circle key={`p${cycle}-${i}`} cx={x} cy={y - 2} r="8" className={`sc-pulse ${ask && !reduce ? 'go' : ''}`} style={{ animationDelay: `${i * 160}ms` }} />
        ))}

        {/* floating demand signals */}
        {CHIPS.map((c) => {
          const moving = gather && !c.quiet
          const style = {
            transform: moving ? `translate(${c.dx}px, ${c.dy}px) scale(0.6)` : `translate(0px, ${ask ? -6 : 6}px)`,
            opacity: ask && !moving ? 1 : moving ? 0 : 0,
            transitionDelay: `${moving ? c.d / 2 : c.d}ms`,
          }
          const label = t('scene.needed', { p: pname(c.p.charAt(0).toUpperCase() + c.p.slice(1)) })
          const w = label.length * 7.4 + 22
          return (
            <g key={c.p + c.x} transform={`translate(${c.x} ${c.y})`}>
              <g className="sc-chip" style={style}>
                <rect x={-w / 2} y="-13" width={w} height="26" rx="13" className={c.quiet ? 'quiet' : ''} />
                <text textAnchor="middle" y="4.5">{label}</text>
              </g>
            </g>
          )
        })}

        {/* opportunity card */}
        <g transform="translate(566 52)" className={`sc-card ${lit ? 'show' : ''}`}>
          <rect x="-106" y="-30" width="212" height="60" rx="14" />
          <circle cx="-82" cy="-6" r="9" className="flame" />
          <text x="-62" y="-10" className="t1">{t('scene.high')}</text>
          <text x="-62" y="4" className="t2">{t('scene.a2')} · {t('scene.est')}</text>
          <rect x="-62" y="14" width="150" height="5" rx="2.5" className="bar-track" />
          <rect x="-62" y="14" width="150" height="5" rx="2.5" className="bar-fill" style={{ transform: lit ? 'scaleX(0.82)' : 'scaleX(0)' }} />
        </g>

        {/* route + vendor */}
        <path d={ROUTE} className="sc-route-bg" />
        <path d={ROUTE} className={`sc-route ${drive ? 'show' : ''}`} pathLength="1" />
        <circle cx={END.x} cy={END.y} r="9" className={`sc-dest ${lit ? 'show' : ''}`} />
        <circle cx={END.x} cy={END.y} r="9" className={`sc-dest-ring ${lit ? 'show' : ''}`} /> {/* NEW: pulsing destination marker */}

        <g transform={`translate(${reduce ? END.x : START.x} ${reduce ? END.y : START.y})`}>
          {drive && !reduce ? <Drive key={`drive-${cycle}`} /> : <Cart />}
        </g>

        {/* the vendor has arrived: nearby customers see them */}
        <g transform="translate(566 282)" className={`sc-found ${stage >= 5 ? 'show' : ''}`}>
          <rect x="-104" y="-14" width="208" height="28" rx="14" />
          <circle cx="-86" cy="0" r="5" className="dot" />
          <text x="-74" y="4.5">{t('scene.found')}</text>
        </g>

        {/* NEW: floating marketplace product cards (skipped in the compact variant) */}
        {!compact && FLOATS.map((f) => (
          <FloatCard key={f.p} x={f.x} y={f.y} label={pname(f.p)} color={f.color} d={f.d} b={f.b} />
        ))}
      </svg>

      <ol className="scene-steps" aria-label={t('land.howH')}>
        {STEPS.map((s, i) => (
          <li key={s} className={i === activeStep && stage > 0 ? 'active' : ''}><span>{i + 1}</span>{s}</li>
        ))}
      </ol>
      <figcaption className="scene-note">{t('scene.note')}</figcaption>
    </figure>
  )
}