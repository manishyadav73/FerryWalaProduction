import { useEffect, useRef } from 'react'
import { NavLink, Outlet, useLocation, Link } from 'react-router-dom'
import { useAuth } from '../context/Auth'
import { useCart } from '../context/Cart'
import { useToast } from '../context/Toast'
import { api } from '../lib/api'
import { useState } from 'react'
import Icon from './Icon'
import Logo from './Logo'
import Notifications from './Notifications'
import LanguageSwitcher from './LanguageSwitcher'
import LanguagePicker from './LanguagePicker'
import AssistantLauncher from './AssistantLauncher'
import OfflineBanner from './OfflineBanner'
import NavProgress from './NavProgress'
import { useI18n } from '../i18n'

function tabsFor(user, cartCount, t) {
  if (user?.role === 'VENDOR') {
    return [
      { to: '/vendor', end: true, icon: 'sun', label: t('nav.today') },
      { to: '/vendor/requests', icon: 'list', label: t('nav.requests') },
      { to: '/vendor/products', icon: 'tag', label: t('nav.products') },
      { to: '/vendor/profile', icon: 'user', label: t('nav.profile') },
    ]
  }
  if (user?.role === 'ADMIN') return [{ to: '/admin', icon: 'shield', label: t('nav.admin') }]
  return [
    { to: '/vendors', icon: 'store', label: t('nav.vendors') },
    { to: '/cart', icon: 'cart', label: t('nav.request'), badge: cartCount },
    { to: '/orders', icon: 'receipt', label: t('nav.orders') },
    user ? { to: '/account', icon: 'user', label: t('nav.account') } : { to: '/login', icon: 'user', label: t('nav.signIn') },
  ]
}

function VerifyBanner() {
  const { user } = useAuth()
  const toast = useToast()
  const { t, errMsg } = useI18n()
  const [hidden, setHidden] = useState(false)
  if (!user || user.emailVerified || hidden || user.role === 'ADMIN') return null
  async function resend() {
    try { const r = await api.resendVerification(); toast.info(t('verify.sent')); void r } catch (e) { toast.error(errMsg(e)) }
  }
  return (
    <div className="banner" role="status">
      <span>{t('verify.banner')}</span>
      <button type="button" className="link" onClick={resend}>{t('verify.resend')}</button>
      <button type="button" className="icon-btn" onClick={() => setHidden(true)} aria-label={t('common.close')}><Icon name="x" size={16} /></button>
    </div>
  )
}

export default function Shell() {
  const { user } = useAuth()
  const cart = useCart()
  const { pathname } = useLocation()
  const { t } = useI18n()
  const tabs = tabsFor(user, cart.count, t)
  const isLanding = pathname === '/'
  const main = useRef(null)
  const first = useRef(true)

  // On navigation: back to the top and focus the page for keyboard and screen-reader users.
  // Hash links (#faq) are left alone.
  useEffect(() => {
    if (first.current) { first.current = false; return }
    if (!window.location.hash) window.scrollTo({ top: 0, behavior: 'instant' })
    main.current?.focus({ preventScroll: true })
  }, [pathname])

  return (
    <div className="shell">
      <header className="topbar">
        <Link to="/" className="brand" aria-label={t('nav.home')}><Logo /></Link>
        <nav className="topnav" aria-label={t('nav.main')}>
          {tabs.map((t) => (
            <NavLink key={t.to} to={t.to} end={t.end} className={({ isActive }) => (isActive ? 'active' : '')}>
              {t.label}{t.badge > 0 && <span className="badge inline bump" key={t.badge}>{t.badge}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="top-actions">
          <LanguageSwitcher />
          <Notifications />
          {!user && <Link className="btn btn-primary btn-sm hide-sm" to="/register?role=VENDOR">{t('nav.joinVendor')}</Link>}
        </div>
      </header>
      <NavProgress />
      <OfflineBanner />
      <VerifyBanner />
      <main id="main" className="main" tabIndex={-1} ref={main} key={pathname}><Outlet /></main>
      <LanguagePicker />
      {(user?.role === 'CUSTOMER' || user?.role === 'VENDOR') && (
        <AssistantLauncher role={user.role} hidden={isLanding || pathname === '/cart' || pathname.startsWith('/meet/')} />
      )}
      {!isLanding && (
        <nav className="bottomnav" aria-label={t('nav.main')}>
          {tabs.map((t) => (
            <NavLink key={t.to} to={t.to} end={t.end} className={({ isActive }) => (isActive ? 'active' : '')}>
              <span className="bn-icon"><Icon name={t.icon} size={22} />{t.badge > 0 && <span className="badge bump" key={t.badge}>{t.badge}</span>}</span>
              <span>{t.label}</span>
            </NavLink>
          ))}
        </nav>
      )}
    </div>
  )
}
