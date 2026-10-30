import { useState } from 'react'
import { OrderStatusPill } from '../components/Pills'
import { EmptyState, ErrorState, ListSkeleton } from '../components/States'
import { useToast } from '../context/Toast'
import { api } from '../lib/api'
import { money, timeAgo } from '../lib/format'
import { useAsync } from '../lib/useAsync'
import { useTitle } from '../lib/useTitle'

const TABS = [['overview', 'Overview'], ['users', 'Users'], ['vendors', 'Vendors'], ['products', 'Products'], ['orders', 'Orders'], ['demand', 'Demand areas'], ['flags', 'Flags'], ['support', 'Support']]

function Load({ state, children }) {
  if (state.loading && !state.data) return <ListSkeleton count={2} />
  if (state.error) return <ErrorState error={state.error} onRetry={state.reload} />
  return children(state.data)
}

function Overview() {
  const s = useAsync((sig) => api.adminStats(sig), [])
  return (
    <Load state={s}>
      {(d) => (
        <div className="stack">
          <div className="stats stats-wide">
            {[['Users', d.users], ['Customers', d.customers], ['Vendors', d.vendors], ['Verified', d.verifiedVendors], ['Online now', d.onlineVendors], ['Products', d.products], ['Orders', d.orders], ['Reviews', d.reviews], ['Signals (7d)', d.demandSignalsLast7Days], ['Completed revenue', money(d.completedRevenue)]].map(([k, v]) => (
              <div key={k} className="stat"><b>{v}</b><span>{k}</span></div>
            ))}
          </div>
          <div className="card stack-sm"><h3>Orders by status</h3>
            <div className="chips">{Object.entries(d.ordersByStatus).map(([k, v]) => <span key={k} className="chip chip-green">{k.replaceAll('_', ' ')}: {v}</span>)}</div>
          </div>
        </div>
      )}
    </Load>
  )
}

function Users() {
  const toast = useToast()
  const [q, setQ] = useState('')
  const s = useAsync((sig) => api.adminUsers({ q: q.trim() || undefined }, sig), [q.trim()])
  async function toggle(u) {
    try { await api.adminSetUserEnabled(u.id, !u.enabled); s.reload() } catch (e) { toast.error(e.message) }
  }
  return (
    <div className="stack-sm">
      <input className="input" type="search" placeholder="Search name, username or email" aria-label="Search users" value={q} onChange={(e) => setQ(e.target.value)} />
      <Load state={s}>
        {(d) => d.items.length === 0 ? <EmptyState icon="users" title="No users match" /> : (
          <ul className="plain stack-sm">{d.items.map((u) => (
            <li key={u.id} className="card flat spread">
              <div className="grow"><b>{u.name}</b> <span className="muted">@{u.username}</span><div className="muted small">{u.email} · {u.role}{u.emailVerified ? '' : ' · unverified'} · joined {timeAgo(u.createdAt)}</div></div>
              {u.role !== 'ADMIN' && <button type="button" className={`btn btn-sm ${u.enabled ? 'btn-danger' : 'btn-primary'}`} onClick={() => toggle(u)}>{u.enabled ? 'Suspend' : 'Restore'}</button>}
            </li>
          ))}</ul>
        )}
      </Load>
    </div>
  )
}

function Vendors() {
  const toast = useToast()
  const s = useAsync((sig) => api.adminVendors({}, sig), [])
  async function toggle(v) {
    try { await api.adminSetVendorVerified(v.id, !v.verified); s.reload() } catch (e) { toast.error(e.message) }
  }
  return (
    <Load state={s}>
      {(d) => d.items.length === 0 ? <EmptyState icon="store" title="No vendors yet" /> : (
        <ul className="plain stack-sm">{d.items.map((v) => (
          <li key={v.id} className="card flat spread">
            <div className="grow"><b>{v.shopName}</b> {v.verified && <span className="verified">Verified</span>}<div className="muted small">{v.ownerName} · {v.ownerEmail} · {v.online ? 'online' : 'offline'}{v.enabled ? '' : ' · suspended'}</div></div>
            <button type="button" className="btn btn-sm btn-ghost" onClick={() => toggle(v)}>{v.verified ? 'Remove badge' : 'Verify'}</button>
          </li>
        ))}</ul>
      )}
    </Load>
  )
}

function Products() {
  const toast = useToast()
  const s = useAsync((sig) => api.adminProducts({}, sig), [])
  async function remove(p) {
    if (!window.confirm(`Remove ${p.name} from ${p.vendorName}?`)) return
    try { await api.adminDeleteProduct(p.id); s.reload() } catch (e) { toast.error(e.message) }
  }
  return (
    <Load state={s}>
      {(d) => d.items.length === 0 ? <EmptyState icon="tag" title="No products yet" /> : (
        <ul className="plain stack-sm">{d.items.map((p) => (
          <li key={p.id} className="card flat spread">
            <div className="grow"><b>{p.name}</b><div className="muted small">{p.vendorName} · {money(p.price)}/{p.unit} · {p.stock} in stock</div></div>
            <button type="button" className="btn btn-sm btn-danger" onClick={() => remove(p)}>Remove</button>
          </li>
        ))}</ul>
      )}
    </Load>
  )
}

function Orders() {
  const s = useAsync((sig) => api.adminOrders('', sig), [])
  return (
    <Load state={s}>
      {(d) => d.length === 0 ? <EmptyState icon="receipt" title="No orders yet" /> : (
        <ul className="plain stack-sm">{d.map((o) => (
          <li key={o.id} className="card flat spread">
            <div className="grow"><b>#{o.id}</b> {o.customerName} → {o.vendorName}<div className="muted small">{money(o.totalAmount)} · {timeAgo(o.createdAt)}</div></div>
            <OrderStatusPill status={o.status} />
          </li>
        ))}</ul>
      )}
    </Load>
  )
}

function Demand() {
  const s = useAsync((sig) => api.adminDemandAreas(sig), [])
  return (
    <Load state={s}>
      {(d) => d.length === 0 ? <EmptyState icon="map" title="No demand data yet">Areas appear once customers send requests or place orders.</EmptyState> : (
        <ul className="plain stack-sm">{d.map((a) => (
          <li key={a.areaKey} className="card flat spread">
            <div className="grow"><b>{a.areaName}</b><div className="muted small">{a.topProducts.slice(0, 3).map((p) => p.name).join(', ')} · {a.signalCount} signals · {a.customerCount} customers</div></div>
            <span className="pill">{a.level} · {a.score}</span>
          </li>
        ))}</ul>
      )}
    </Load>
  )
}

function Flags() {
  const s = useAsync((sig) => api.adminSuspicious(sig), [])
  return (
    <Load state={s}>
      {(d) => d.length === 0 ? <EmptyState icon="shield" title="Nothing suspicious right now">Flags are prompts to take a look, not verdicts.</EmptyState> : (
        <ul className="plain stack-sm">{d.map((f, i) => (
          <li key={i} className="card flat"><div className="spread"><b>{f.kind.replaceAll('_', ' ')}</b><span className="pill">{f.severity}</span></div><div className="muted small">{f.subject}: {f.detail}</div></li>
        ))}</ul>
      )}
    </Load>
  )
}

const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

function CaseRow({ c, onSaved }) {
  const toast = useToast()
  const [status, setStatus] = useState(c.status)
  const [note, setNote] = useState(c.resolutionNote || '')
  const [busy, setBusy] = useState(false)
  async function save() {
    setBusy(true)
    try { await api.adminUpdateCase(c.id, { status, resolutionNote: note.trim() || null }); toast.success('Case updated.'); onSaved() } catch (e) { toast.error(e.message) } finally { setBusy(false) }
  }
  return (
    <li className="card flat stack-sm">
      <div className="spread"><b>#{c.id} · {c.category.replaceAll('_', ' ')}</b><span className="pill">{c.creatorName} ({c.creatorRole})</span></div>
      <p>{c.summary}</p>
      <div className="muted small">{c.orderId ? `Request #${c.orderId} · ` : ''}{c.source === 'CONCIERGE' ? 'Started from AI Concierge · ' : ''}{timeAgo(c.createdAt)}</div>
      <div className="row-gap">
        <select className="input" value={status} onChange={(e) => setStatus(e.target.value)} aria-label={`Status of case ${c.id}`}>{STATUSES.map((x) => <option key={x}>{x}</option>)}</select>
        <input className="input grow" value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} placeholder="Note shown to the user" aria-label={`Resolution note for case ${c.id}`} />
        <button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={save}>Save</button>
      </div>
    </li>
  )
}

function SupportCases() {
  const [status, setStatus] = useState('')
  const s = useAsync((sig) => api.adminCases(status || undefined, sig), [status])
  return (
    <>
      <select className="input" value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Filter by status">
        <option value="">All statuses</option>{STATUSES.map((x) => <option key={x}>{x}</option>)}
      </select>
      <Load state={s}>
        {(d) => d.length === 0 ? <EmptyState icon="help" title="No support cases">Cases appear when users report a problem.</EmptyState> : (
          <ul className="plain stack-sm">{d.map((c) => <CaseRow key={c.id + c.status + (c.updatedAt || '')} c={c} onSaved={s.reload} />)}</ul>
        )}
      </Load>
    </>
  )
}

const VIEWS = { support: SupportCases, overview: Overview, users: Users, vendors: Vendors, products: Products, orders: Orders, demand: Demand, flags: Flags }

export default function Admin() {
  useTitle('Admin')
  const [tab, setTab] = useState('overview')
  const View = VIEWS[tab]
  return (
    <div className="page stack">
      <h1 className="h1-sm">Admin</h1>
      <div className="chips scroll-x" role="tablist" aria-label="Admin sections">
        {TABS.map(([k, l]) => <button key={k} type="button" role="tab" aria-selected={tab === k} className={`chip-btn ${tab === k ? 'on' : ''}`} onClick={() => setTab(k)}>{l}</button>)}
      </div>
      <View />
    </div>
  )
}
