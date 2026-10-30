import { useEffect, useRef, useState } from 'react'

/** Numbers glide to their value (about 0.8 s). With reduced motion they simply appear. */
export default function CountUp({ value, format = (n) => String(Math.round(n)) }) {
  const target = Number(value) || 0
  const reduce = typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
  const [shown, setShown] = useState(reduce ? target : 0)
  const from = useRef(reduce ? target : 0)

  useEffect(() => {
    if (reduce) { from.current = target; return undefined }
    const start = performance.now()
    const a = from.current
    let raf
    const tick = (now) => {
      const p = Math.min(1, (now - start) / 800)
      const eased = 1 - (1 - p) ** 3
      const v = a + (target - a) * eased
      setShown(v)
      from.current = v
      if (p < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [target, reduce])

  return <>{format(reduce ? target : shown)}</>
}
