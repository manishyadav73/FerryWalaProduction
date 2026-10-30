import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'

/**
 * Slim bar at the top while a navigation is pending (for example a page's code is still downloading
 * on a slow phone). It starts when an in-app link is clicked, appears only if that takes longer than
 * 150 ms, and ends when the new route renders. It never blocks clicks.
 */
export default function NavProgress() {
  const { pathname } = useLocation()
  const [from, setFrom] = useState(null) // rendered path when the click happened
  const current = useRef(pathname)
  useEffect(() => { current.current = pathname })

  useEffect(() => {
    let timer
    const start = (e) => {
      const a = e.target.closest?.('a[href]')
      if (!a || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || a.target === '_blank') return
      const url = new URL(a.href, window.location.href)
      if (url.origin !== window.location.origin || url.pathname === current.current) return
      clearTimeout(timer)
      const was = current.current
      timer = setTimeout(() => setFrom(was), 150)
    }
    document.addEventListener('click', start, true)
    return () => { document.removeEventListener('click', start, true); clearTimeout(timer) }
  }, [])

  // React keeps showing the old page until the new one is ready, so `pathname` only changes once it is.
  const on = from === pathname

  useEffect(() => {
    if (!on) return undefined
    const t = setTimeout(() => setFrom(null), 10000) // never get stuck
    return () => clearTimeout(t)
  }, [on])

  return on ? <div className="route-bar" role="progressbar" aria-label="Loading"><i /></div> : null
}
