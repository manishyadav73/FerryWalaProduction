import { Fragment } from 'react' // NEW
import { Link } from 'react-router-dom'
import Logo from '../components/Logo'
import { useAuth } from '../context/Auth'
import Icon from '../components/Icon'
import LivingScene from '../components/LivingScene'
import HeroBackdrop from '../components/HeroBackdrop' // NEW
import HeroSearch from '../components/HeroSearch'
import Reveal from '../components/Reveal'
import Tilt from '../components/Tilt'
import DemandSimulator from '../components/DemandSimulator'
import { homeFor } from '../components/ProtectedRoute'
import { useI18n } from '../i18n'
import { useTitle } from '../lib/useTitle'
import '../styles/hero-cinematic.css' // NEW

export default function Landing() {
  const { t } = useI18n()
  useTitle('')
  const { user } = useAuth()
  const how = [1, 2, 3, 4].map((n) => ({ n, t: t(`land.how${n}t`), d: t(`land.how${n}d`) }))
  const signals = [1, 2, 3, 4].map((n) => [t(`land.sig${n}t`), t(`land.sig${n}d`)])
  const custTicks = [1, 2, 3, 4].map((n) => t(`land.cust${n}`))
  const vendTicks = [1, 2, 3, 4].map((n) => t(`land.vend${n}`))
  const faqs = [1, 2, 3, 4, 5, 6].map((n) => ({ q: t(`land.faq${n}q`), a: t(`land.faq${n}a`) }))
  const flow = [t('land.flow1'), t('land.flow2'), t('land.flow3'), t('land.flow4')]
  const h1 = t('land.h1') // NEW
  const h1Words = h1.split(' ') // NEW

  return (
    <div className="landing">
      <section className="hero-stage"> {/* NEW wrapper: full-bleed cinematic stage */}
        <HeroBackdrop />
        <div className="hero">
          <div className="hero-copy">
            <p className="eyebrow">{t('land.eyebrow')}</p>
            <h1 aria-label={h1}>
              {h1Words.map((w, i) => (
                <Fragment key={`${w}-${i}`}>
                  <span className="hw" aria-hidden="true" style={{ '--w': i }}><span>{w}</span></span>
                  {i < h1Words.length - 1 ? ' ' : null}
                </Fragment>
              ))}
            </h1>
            <p className="lead">{t('land.lead')}</p>
            <HeroSearch />
            <div className="cta-row">
              <Link className="btn btn-primary btn-lg" to="/vendors"><Icon name="pin" size={20} /> {t('land.ctaFind')}</Link>
              <Link className="btn btn-ghost btn-lg" to={user ? homeFor(user) : '/register?role=VENDOR'}>
                {user?.role === 'VENDOR' ? t('land.ctaDashboard') : t('nav.joinVendor')}
              </Link>
            </div>
            <ul className="hero-points">
              <li><Icon name="check" size={16} stroke={2.4} /> {t('land.pt1')}</li>
              <li><Icon name="check" size={16} stroke={2.4} /> {t('land.pt2')}</li>
              <li><Icon name="check" size={16} stroke={2.4} /> {t('land.pt3')}</li>
            </ul>
          </div>
          <Tilt className="hero-scene"><LivingScene /></Tilt>
        </div>
      </section>

      <Reveal as="section" className="band band-problem">
        <div className="wrap two">
          <div>
            <p className="eyebrow">{t('land.problem')}</p>
            <h2>{t('land.problemH')}</h2>
          </div>
          <p className="big-p">{t('land.problemP')}</p>
        </div>
      </Reveal>

      <Reveal as="section" className="wrap section">
        <div className="section-head">
          <p className="eyebrow">{t('land.solution')}</p>
          <h2>{t('land.solutionH')}</h2>
        </div>
        <div className="flow" aria-label={flow.join(' → ')}>
          {flow.map((label, i, a) => (
            <Reveal className="flow-step" key={label} i={i}>
              <span className="flow-dot">{i + 1}</span>
              <b>{label}</b>
              {i < a.length - 1 && <Icon name="chevron" className="flow-arrow" />}
            </Reveal>
          ))}
        </div>
      </Reveal>

      <Reveal as="section" className="wrap section" id="try">
        <div className="section-head">
          <p className="eyebrow">{t('sim.eyebrow')}</p>
          <h2>{t('sim.title')}</h2>
          <p className="muted">{t('sim.sub')}</p>
        </div>
        <DemandSimulator />
      </Reveal>

      <Reveal as="section" className="wrap section" id="how">
        <div className="section-head"><p className="eyebrow">{t('land.howEyebrow')}</p><h2>{t('land.howH')}</h2></div>
        <ol className="how-grid">
          {how.map((h, i) => (
            <Reveal as="li" i={i} key={h.n} className="card how-card"><span className="how-n">{h.n}</span><h3>{h.t}</h3><p className="muted">{h.d}</p></Reveal>
          ))}
        </ol>
      </Reveal>

      <section className="wrap section split">
        <Reveal as="article" className="card benefit">
          <span className="chip chip-green"><Icon name="store" size={16} /> {t('land.forCust')}</span>
          <h3>{t('land.custH')}</h3>
          <ul className="ticks">{custTicks.map((x) => <li key={x}>{x}</li>)}</ul>
          <Link className="btn btn-primary" to="/vendors">{t('land.ctaFindShort')}</Link>
        </Reveal>
        <Reveal as="article" i={1} className="card benefit benefit-vendor">
          <span className="chip chip-saffron"><Icon name="sun" size={16} /> {t('land.forVend')}</span>
          <h3>{t('land.vendH')}</h3>
          <ul className="ticks">{vendTicks.map((x) => <li key={x}>{x}</li>)}</ul>
          <Link className="btn btn-saffron" to="/register?role=VENDOR">{t('nav.joinVendor')}</Link>
        </Reveal>
      </section>

      <Reveal as="section" className="band band-dark">
        <div className="wrap two">
          <div>
            <p className="eyebrow eyebrow-light">{t('land.intel')}</p>
            <h2>{t('land.intelH')}</h2>
            <p className="lead-light">{t('land.intelP')}</p>
          </div>
          <ul className="signal-list">
            {signals.map(([a, d], i) => <Reveal as="li" i={i} key={a}><b>{a}</b><span>{d}</span></Reveal>)}
          </ul>
        </div>
      </Reveal>

      <section className="wrap section trust">
        <div className="section-head"><p className="eyebrow">{t('land.trust')}</p><h2>{t('land.trustH')}</h2></div>
        <div className="trust-grid">
          <Reveal className="card"><Icon name="shield" /><h3>{t('land.tr1t')}</h3><p className="muted">{t('land.tr1d')}</p></Reveal>
          <Reveal i={1} className="card"><Icon name="pin" /><h3>{t('land.tr2t')}</h3><p className="muted">{t('land.tr2d')}</p></Reveal>
          <Reveal i={2} className="card"><Icon name="check" /><h3>{t('land.tr3t')}</h3><p className="muted">{t('land.tr3d')}</p></Reveal>
        </div>
      </section>

      <section className="wrap section" id="faq">
        <div className="section-head"><p className="eyebrow">{t('land.faq')}</p><h2>{t('land.faqH')}</h2></div>
        <div className="faq">
          {faqs.map((f) => (
            <details key={f.q}><summary>{f.q}</summary><p>{f.a}</p></details>
          ))}
        </div>
      </section>

      <Reveal as="section" className="wrap section">
        <div className="cta-card">
          <h2>{t('land.finalH')}</h2>
          <div className="cta-row">
            <Link className="btn btn-primary btn-lg" to="/vendors">{t('land.ctaFind')}</Link>
            <Link className="btn btn-saffron btn-lg" to="/register?role=VENDOR">{t('nav.joinVendor')}</Link>
          </div>
        </div>
      </Reveal>

      <footer className="footer">
        <div className="wrap">
          <div className="footer-grid">
            <div>
              <Logo />
              <p className="muted" style={{ marginTop: 10 }}>{t('land.lead')}</p>
            </div>
            <nav aria-label={t('land.explore')}>
              <h3>{t('land.explore')}</h3>
              <ul>
                <li><Link to="/vendors">{t('land.ctaFindShort')}</Link></li>
                <li><Link to="/register?role=VENDOR">{t('nav.joinVendor')}</Link></li>
                <li><a href="#faq">{t('land.faq')}</a></li>
              </ul>
            </nav>
            <nav aria-label={t('land.account')}>
              <h3>{t('land.account')}</h3>
              <ul>
                <li><Link to="/login">{t('nav.signIn')}</Link></li>
                <li><Link to="/register">{t('auth.createAccount')}</Link></li>
                <li><Link to="/privacy">{t('land.privacy')}</Link></li>
              </ul>
            </nav>
          </div>
          <p className="footer-note">&copy; {new Date().getFullYear()} FerryWala. {t('land.footerNote')}</p>
        </div>
      </footer>
    </div>
  )
}