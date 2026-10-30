import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../i18n'
import Icon from './Icon'

/** Small menu in the top bar. Languages are shown in their own script so people can find theirs. */
export default function LanguageSwitcher() {
  const { lang, setLang, languages, t } = useI18n()
  const [open, setOpen] = useState(false)
  const box = useRef(null)
  const current = languages.find((l) => l.code === lang)

  useEffect(() => {
    if (!open) return undefined
    const onDown = (e) => { if (box.current && !box.current.contains(e.target)) setOpen(false) }
    const onKey = (e) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => { document.removeEventListener('mousedown', onDown); document.removeEventListener('keydown', onKey) }
  }, [open])

  return (
    <div className="lang" ref={box}>
      <button type="button" className="lang-btn" onClick={() => setOpen((o) => !o)} aria-expanded={open} aria-label={t('lang.change')}>
        <Icon name="globe" size={18} /> <span>{current.native}</span>
      </button>
      {open && (
        <ul className="sheet-pop lang-pop" role="listbox" aria-label={t('lang.change')}>
          {languages.map((l) => (
            <li key={l.code}>
              <button type="button" role="option" aria-selected={l.code === lang} className={l.code === lang ? 'on' : ''} onClick={() => { setLang(l.code); setOpen(false) }}>
                <b>{l.native}</b>{l.code !== 'en' && <small>{l.label}</small>}
                {l.code === lang && <Icon name="check" size={16} stroke={2.4} />}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
