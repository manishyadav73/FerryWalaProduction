import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useI18n } from '../i18n'
import Icon from './Icon'

const QUICK = ['tomato', 'mango', 'flowers', 'tea']

/** Glass search panel on the home page. Sends the text to the vendor list, which does the real search. */
export default function HeroSearch() {
  const { t } = useI18n()
  const nav = useNavigate()
  const [q, setQ] = useState('')
  const go = (text) => nav(`/vendors${text.trim() ? `?q=${encodeURIComponent(text.trim().slice(0, 60))}` : ''}`)
  return (
    <form className="hero-search glass" role="search" onSubmit={(e) => { e.preventDefault(); go(q) }}>
      <label className="sr-only" htmlFor="hs-q">{t('hero.searchLabel')}</label>
      <Icon name="search" size={20} />
      <input id="hs-q" className="hs-input" value={q} maxLength={60} onChange={(e) => setQ(e.target.value)} placeholder={t('hero.searchPlaceholder')} autoComplete="off" />
      <button className="btn btn-primary" type="submit">{t('hero.searchGo')}</button>
      <div className="hs-quick" role="group" aria-label={t('hero.quick')}>
        {QUICK.map((k) => <button key={k} type="button" className="chip-btn" onClick={() => go(t(`hero.q.${k}`))}>{t(`hero.q.${k}`)}</button>)}
      </div>
    </form>
  )
}
