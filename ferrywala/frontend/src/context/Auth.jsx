import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, setUnauthorizedHandler, tokenStore } from '../lib/api'
import { disconnect } from '../lib/realtime'
import { useToast } from './Toast'
import { useI18n } from '../i18n'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const toast = useToast()
  const { t } = useI18n()
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(!tokenStore.get())

  const signOut = useCallback(() => {
    tokenStore.set(null)
    disconnect()
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      signOut()
      toast.info(t('err.signInAgain'))
    })
  }, [signOut, toast, t])

  useEffect(() => {
    if (!tokenStore.get()) return undefined
    const controller = new AbortController()
    api.me(controller.signal)
      .then(setUser)
      .catch((e) => {
        if (e.code === 'ABORTED') return
        if (e.status === 401 || e.status === 403) tokenStore.set(null)
      })
      .finally(() => !controller.signal.aborted && setReady(true))
    return () => controller.abort()
  }, [])

  const finishAuth = useCallback((res) => {
    if (res.token) {
      tokenStore.set(res.token)
      setUser(res.user)
    }
    return res
  }, [])

  const value = useMemo(() => ({
    user,
    ready,
    isCustomer: user?.role === 'CUSTOMER',
    isVendor: user?.role === 'VENDOR',
    isAdmin: user?.role === 'ADMIN',
    login: async (identifier, password) => finishAuth(await api.login({ identifier, password })),
    register: async (body) => finishAuth(await api.register(body)),
    logout: signOut,
    refresh: async () => setUser(await api.me()),
  }), [user, ready, finishAuth, signOut])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext)
}
