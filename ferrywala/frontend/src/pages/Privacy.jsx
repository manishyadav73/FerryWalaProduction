import { Link } from 'react-router-dom'
import { useI18n } from '../i18n'
import { useTitle } from '../lib/useTitle'

// Written from what the code actually does. Have a lawyer review before treating it as a legal policy.
const SECTIONS = [
  ['What we collect', [
    'Account details: your name, username, email and a salted hash of your password (we never store the password itself).',
    'Vendors: shop name, category, description, address or area, stall location, and an optional photo.',
    'Requests and orders you send: the items, a note, and the meeting point you choose.',
    'Demand signals: when you ask for a product, search, or place an order, we record the product, the location you chose or shared, and the time. Vendors only ever see this combined by area.',
  ]],
  ['Your location', [
    'We ask for your location only when you tap a button that needs it, such as "Use my current location". We do not track you in the background.',
    'You can search for your locality instead of sharing your location.',
    'Vendors choose when to share their stall location and can go offline at any time.',
  ]],
  ['What vendors can see', [
    'Vendors see demand grouped by area and product. They do not see household purchase history or who asked.',
    'When you send a vendor a request, they see your name, the items and the meeting point you chose for that request.',
  ]],
  ['How we use data', [
    'To show nearby vendors, run requests, and estimate where customers are likely to need products. Estimates are never guarantees.',
    'We send emails only for account confirmation and password reset.',
    'We do not sell your data.',
  ]],
  ['Payments', ['FerryWala does not process payments. You pay the vendor directly.']],
  ['Your choices', [
    'Update your profile, or remove a vendor photo, in the app.',
    'To delete your account or data, contact the operator of this FerryWala service.',
  ]],
]

export default function Privacy() {
  const { t } = useI18n()
  useTitle(t('privacy.title'))
  return (
    <div className="page narrow stack">
      <h1>{t('privacy.title')}</h1>
      <p className="note small">{t('privacy.updated')}</p>
      {SECTIONS.map(([h, items]) => (
        <section key={h} className="stack-sm">
          <h2 className="h2-sm">{h}</h2>
          <ul className="ticks">{items.map((x) => <li key={x}>{x}</li>)}</ul>
        </section>
      ))}
      <p><Link to="/">&larr; FerryWala</Link></p>
    </div>
  )
}
