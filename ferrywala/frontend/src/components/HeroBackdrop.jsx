import { useEffect, useRef } from 'react'
import { useReducedMotion } from '../lib/motion'

/*
 * Decorative cinematic backdrop for the landing hero: soft light blobs, a far market skyline with
 * stalls, bunting, and drifting particles. Pure SVG + CSS. The only JS is a throttled pointer
 * parallax (fine pointers only) and an off-screen pause. Everything is aria-hidden.
 */

const PARTICLES = Array.from({ length: 18 }, (_, i) => ({
  l: (i * 53) % 100,
  t: 18 + ((i * 37) % 72),
  s: 3 + (i % 4) * 1.5,
  d: -(i * 1.3),
  dur: 9 + (i % 5) * 2.5,
}))

// bunting pennants sit on a gentle sagging line: y = 14 + 84·u(1-u), u = position inside each 720px span
const FLAGS = Array.from({ length: 24 }, (_, i) => {
  const x = i * 60 + 30
  const u = (x % 720) / 720
  return { x, y: 14 + 84 * u * (1 - u), k: i % 3 }
})

const STALLS = [120, 430, 770, 1100, 1330]

export default function HeroBackdrop() {
  const root = useRef(null)
  const reduce = useReducedMotion()

  // pause every animation while the hero is off-screen or the tab is hidden
  useEffect(() => {
    const el = root.current
    if (!el || typeof IntersectionObserver === 'undefined') return undefined
    let onScreen = true
    const sync = () => el.setAttribute('data-paused', String(!onScreen || document.hidden))
    const io = new IntersectionObserver(([e]) => { onScreen = e.isIntersecting; sync() }, { threshold: 0.05 })
    io.observe(el)
    document.addEventListener('visibilitychange', sync)
    return () => { io.disconnect(); document.removeEventListener('visibilitychange', sync) }
  }, [])

  // pointer parallax: writes two CSS variables, no React state, one rAF at a time
  useEffect(() => {
    const el = root.current
    const stage = el?.parentElement
    if (!el || !stage || reduce) return undefined
    if (!window.matchMedia?.('(hover: hover) and (pointer: fine)').matches) return undefined
    let raf = 0
    let nx = 0
    let ny = 0
    const apply = () => {
      raf = 0
      el.style.setProperty('--px', nx.toFixed(3))
      el.style.setProperty('--py', ny.toFixed(3))
    }
    const queue = () => { if (!raf) raf = requestAnimationFrame(apply) }
    const move = (e) => {
      const r = stage.getBoundingClientRect()
      nx = ((e.clientX - r.left) / r.width - 0.5) * 2
      ny = ((e.clientY - r.top) / r.height - 0.5) * 2
      queue()
    }
    const leave = () => { nx = 0; ny = 0; queue() }
    stage.addEventListener('pointermove', move, { passive: true })
    stage.addEventListener('pointerleave', leave)
    return () => {
      stage.removeEventListener('pointermove', move)
      stage.removeEventListener('pointerleave', leave)
      if (raf) cancelAnimationFrame(raf)
    }
  }, [reduce])

  return (
    <div className="hb" ref={root} aria-hidden="true">
      <div className="hb-glow hb-glow-a" />
      <div className="hb-glow hb-glow-b" />
      <div className="hb-glow hb-glow-c" />

      <div className="hb-layer hb-bunting">
        <svg viewBox="0 0 1440 90" preserveAspectRatio="xMidYMin slice" focusable="false">
          <path d="M0 14Q360 56 720 14Q1080 56 1440 14" className="bt-line" />
          <g className="bt-flags">
            {FLAGS.map((f) => (
              <path key={f.x} d={`M${f.x - 9} ${f.y}H${f.x + 9}L${f.x} ${f.y + 20}Z`} className={`bf bf-${f.k}`} />
            ))}
          </g>
        </svg>
      </div>

      <div className="hb-layer hb-far">
        <svg viewBox="0 0 1440 240" preserveAspectRatio="xMidYMax slice" focusable="false">
          <path className="sk-far" d="M0 240V150h60v-30h50v30h40v-60l25-20 25 20v60h60v-40h70v40h50v-70h30v-20h30v20h30v70h60v-30h70v30h40v-50l30-30 30 30v50h70v-40h60v40h50v-80h40v80h60v-30h60v30h70v-60h50v60h50v-40h60v40h60V240z" />
          <path className="sk-mid" d="M0 240V196h90v-24h60v24h70v-34h50v34h90v-20h70v20h60v-40h40v40h80v-26h70v26h90v-36h50v36h70v-22h80v22h60v-30h50v30h90v-24h60v24h60V240z" />
          {STALLS.map((x) => (
            <g key={x} transform={`translate(${x} 176)`} className="stall">
              <path d="M-34 0h68l-6 18h-56z" className="st-awn" />
              <path d="M-34 0h17l-2 18h-12zM0 0h17l-1 18h-12z" className="st-stripe" />
              <path d="M-26 18v38M26 18v38" className="st-pole" />
            </g>
          ))}
        </svg>
      </div>

      <div className="hb-particles">
        {PARTICLES.map((p, i) => (
          <i key={i} className="hb-p" style={{ '--l': p.l, '--t': p.t, '--s': `${p.s}px`, animationDelay: `${p.d}s`, animationDuration: `${p.dur}s` }} />
        ))}
      </div>
    </div>
  )
}