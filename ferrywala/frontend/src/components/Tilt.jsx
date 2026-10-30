import { useRef } from 'react'
import { useReducedMotion } from '../lib/motion'

/**
 * Light CSS 3D: the child leans a few degrees toward the pointer, with perspective. No WebGL, no
 * download. Disabled for touch, coarse pointers and reduced motion.
 */
export default function Tilt({ children, max = 5, className = '' }) {
  const box = useRef(null)
  const reduce = useReducedMotion()
  const move = (e) => {
    if (reduce || e.pointerType === 'touch') return
    const el = box.current
    const r = el.getBoundingClientRect()
    const x = (e.clientX - r.left) / r.width - 0.5
    const y = (e.clientY - r.top) / r.height - 0.5
    el.style.setProperty('--ry', `${(x * max * 2).toFixed(2)}deg`)
    el.style.setProperty('--rx', `${(-y * max * 2).toFixed(2)}deg`)
  }
  const leave = () => { box.current?.style.setProperty('--ry', '0deg'); box.current?.style.setProperty('--rx', '0deg') }
  return (
    <div className={`tilt ${className}`} onPointerMove={move} onPointerLeave={leave} ref={box}>
      <div className="tilt-in">{children}</div>
    </div>
  )
}
