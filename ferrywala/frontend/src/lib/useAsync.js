import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Loads data with loading / error / data states and aborts on unmount or reload.
 * `fn(signal)` must return a promise. Re-runs when `deps` change.
 */
export function useAsync(fn, deps = []) {
  const [state, setState] = useState({ loading: true, error: null, data: null })
  const fnRef = useRef(fn)
  const [tick, setTick] = useState(0)

  useEffect(() => {
    fnRef.current = fn
  })

  useEffect(() => {
    const controller = new AbortController()
    let live = true
    Promise.resolve()
      .then(() => {
        if (live) setState((s) => ({ ...s, loading: true, error: null }))
        return fnRef.current(controller.signal)
      })
      .then((data) => live && setState({ loading: false, error: null, data }))
      .catch((error) => {
        if (!live || error?.code === 'ABORTED') return
        setState((s) => ({ loading: false, error, data: s.data }))
      })
    return () => {
      live = false
      controller.abort()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])
  const setData = useCallback((updater) => {
    setState((s) => ({ ...s, data: typeof updater === 'function' ? updater(s.data) : updater }))
  }, [])
  return { ...state, reload, setData }
}
