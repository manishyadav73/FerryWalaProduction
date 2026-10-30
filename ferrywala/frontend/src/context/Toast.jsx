import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [items, setItems] = useState([])
  const idRef = useRef(0)

  const dismiss = useCallback((id) => setItems((list) => list.filter((t) => t.id !== id)), [])
  const push = useCallback((kind, message, ms = 4200) => {
    const id = ++idRef.current
    setItems((list) => [...list.slice(-2), { id, kind, message }])
    setTimeout(() => dismiss(id), ms)
  }, [dismiss])

  const api = useMemo(() => ({
    success: (m) => push('success', m),
    error: (m) => push('error', m, 6000),
    info: (m) => push('info', m),
  }), [push])

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {items.map((t) => (
          <button key={t.id} type="button" className={`toast toast-${t.kind}`} onClick={() => dismiss(t.id)}>
            {t.message}
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
  return useContext(ToastContext)
}
