import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import en from './en'
import hi from './hi'
import ta from './ta'
import { PRODUCT_NAMES } from './products'

const DICTS = { en, hi, ta }
const KEY = 'fw_lang'

/** Add a language: create i18n/<code>.js with the same keys, import it above, and list it here. */
// eslint-disable-next-line react-refresh/only-export-components
export const LANGUAGES = [
  { code: 'en', label: 'English', native: 'English', speech: 'en-IN' },
  { code: 'hi', label: 'Hindi', native: 'हिन्दी', speech: 'hi-IN' },
  { code: 'ta', label: 'Tamil', native: 'தமிழ்', speech: 'ta-IN' },
]

const I18nContext = createContext(null)

function stored() {
  try {
    const v = localStorage.getItem(KEY)
    return DICTS[v] ? v : null
  } catch { return null }
}

function guess() {
  const n = (typeof navigator !== 'undefined' && navigator.language) || 'en'
  const c = n.slice(0, 2).toLowerCase()
  return DICTS[c] ? c : 'en'
}

const fill = (s, vars) => (vars ? s.replace(/\{(\w+)\}/g, (_, k) => (vars[k] ?? `{${k}}`)) : s)

// Words for the part of day, used to say "5 PM" the way people actually say it.
function hourWord(lang, h) {
  const p = h >= 4 && h < 12 ? 0 : h >= 12 && h < 16 ? 1 : h >= 16 && h < 20 ? 2 : 3
  return t0(lang, `time.part${p}`)
}
function t0(lang, key) {
  return DICTS[lang][key] ?? en[key] ?? key
}

export function LanguageProvider({ children }) {
  const [saved, setSaved] = useState(stored)
  const lang = saved ?? guess()

  useEffect(() => {
    document.documentElement.lang = lang
  }, [lang])

  const setLang = useCallback((code) => {
    if (!DICTS[code]) return
    setSaved(code)
    try { localStorage.setItem(KEY, code) } catch { /* private mode */ }
  }, [])

  const value = useMemo(() => {
    const t = (key, vars) => fill(DICTS[lang][key] ?? en[key] ?? key, vars)
    const hour = (h) => {
      const x = ((Math.round(h) % 24) + 24) % 24
      const h12 = x % 12 === 0 ? 12 : x % 12
      if (lang === 'en') return `${h12} ${x < 12 ? 'AM' : 'PM'}`
      return fill(t0(lang, 'time.hour'), { part: hourWord(lang, x), n: h12 })
    }
    return {
      lang,
      chosen: saved !== null,
      setLang,
      languages: LANGUAGES,
      speechLang: LANGUAGES.find((l) => l.code === lang).speech,
      t,
      has: (key) => key in DICTS[lang] || key in en,
      hour,
      windowLabel: (w) => (w ? `${hour(w.startHour)} – ${hour(w.endHour)}` : ''),
      pname: (name) => {
        if (!name || lang === 'en') return name
        const hit = PRODUCT_NAMES[String(name).trim().toLowerCase()]
        return hit?.[lang] ?? name
      },
      errMsg: (err) => (err?.code && key_(`err.${err.code}`, lang) ? t(`err.${err.code}`) : err?.message || t('err.generic')),
      ago: (iso) => {
        if (!iso) return ''
        const s = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000)
        if (s < 45) return t('time.justNow')
        if (s < 3600) return t('time.min', { n: Math.round(s / 60) })
        if (s < 86400) return t('time.hr', { n: Math.round(s / 3600) })
        return t('time.day', { n: Math.round(s / 86400) })
      },
      dist: (d) => (d == null ? '' : d < 1 ? `${Math.round(d * 1000)} ${t('unit.m')}` : `${d.toFixed(1)} ${t('unit.km')}`),
    }
  }, [lang, saved, setLang])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

function key_(key, lang) {
  return key in DICTS[lang] || key in en
}

// eslint-disable-next-line react-refresh/only-export-components
export function useI18n() {
  return useContext(I18nContext)
}
