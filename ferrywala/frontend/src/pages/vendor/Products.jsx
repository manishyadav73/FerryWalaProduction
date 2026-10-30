import { useState } from 'react'
import Icon from '../../components/Icon'
import { EmptyState, ErrorState, ListSkeleton } from '../../components/States'
import { useToast } from '../../context/Toast'
import { api } from '../../lib/api'
import { CATEGORIES, QUICK_PRODUCTS } from '../../lib/categories'
import { useI18n } from '../../i18n'
import { money } from '../../lib/format'
import { useAsync } from '../../lib/useAsync'
import { useTitle } from '../../lib/useTitle'

const EMPTY = { name: '', category: 'VEGETABLES', price: '', unit: 'kg', stock: '10' }

function ProductForm({ initial, onSaved, onCancel }) {
  const toast = useToast()
  const { t } = useI18n()
  const [f, setF] = useState(initial)
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState({})
  const set = (k) => (e) => setF((x) => ({ ...x, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    const errs = {}
    if (!f.name.trim()) errs.name = t('prod.vName')
    if (!(Number(f.price) > 0)) errs.price = t('prod.vPrice')
    if (!f.unit.trim()) errs.unit = t('prod.vUnit')
    if (!Number.isInteger(Number(f.stock)) || Number(f.stock) < 0) errs.stock = t('prod.vStock')
    setErrors(errs)
    if (Object.keys(errs).length) return
    setBusy(true)
    const body = { name: f.name.trim(), category: f.category, price: Number(f.price), unit: f.unit.trim(), stock: Number(f.stock), available: true }
    try {
      const saved = f.id ? await api.vendorUpdateProduct(f.id, body) : await api.vendorCreateProduct(body)
      toast.success(f.id ? t('prod.updated') : t('prod.added'))
      onSaved(saved)
    } catch (err) {
      setErrors(err.details && Object.keys(err.details).length ? err.details : { form: t('err.generic') })
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card stack-sm" onSubmit={submit} noValidate>
      <h3>{f.id ? t('prod.edit') : t('prod.add')}</h3>
      {errors.form && <p className="form-error" role="alert">{errors.form}</p>}
      <label className="field"><span>{t('prod.name')}</span><input className="input" value={f.name} onChange={set('name')} maxLength={80} aria-invalid={!!errors.name} />{errors.name && <em className="field-error">{errors.name}</em>}</label>
      <label className="field"><span>{t('prod.category')}</span>
        <select className="input" value={f.category} onChange={set('category')}>{CATEGORIES.map((c) => <option key={c.value} value={c.value}>{t(`cat.${c.value}`)}</option>)}</select>
      </label>
      <div className="grid-3">
        <label className="field"><span>{t('prod.price')}</span><input className="input" inputMode="decimal" value={f.price} onChange={set('price')} aria-invalid={!!errors.price} />{errors.price && <em className="field-error">{errors.price}</em>}</label>
        <label className="field"><span>{t('prod.per')}</span><input className="input" value={f.unit} onChange={set('unit')} maxLength={20} aria-invalid={!!errors.unit} />{errors.unit && <em className="field-error">{errors.unit}</em>}</label>
        <label className="field"><span>{t('prod.stock')}</span><input className="input" inputMode="numeric" value={f.stock} onChange={set('stock')} aria-invalid={!!errors.stock} />{errors.stock && <em className="field-error">{errors.stock}</em>}</label>
      </div>
      <div className="row-gap">
        <button className="btn btn-primary" disabled={busy}>{busy ? t('common.saving') : t('common.save')}</button>
        <button type="button" className="btn btn-ghost" onClick={onCancel}>{t('common.cancel')}</button>
      </div>
    </form>
  )
}

export default function Products() {
  const { t, pname, errMsg } = useI18n()
  useTitle(t('nav.products'))
  const toast = useToast()
  const { loading, error, data, reload } = useAsync((s) => api.vendorProducts(s), [])
  const [editing, setEditing] = useState(null)

  async function toggle(p) {
    try { await api.vendorSetAvailability(p.id, !p.available); reload() } catch (e) { toast.error(errMsg(e)) }
  }
  async function remove(p) {
    if (!window.confirm(t('prod.confirmRemove', { name: pname(p.name) }))) return
    try { await api.vendorDeleteProduct(p.id); toast.info(t('prod.removed')); reload() } catch (e) { toast.error(errMsg(e)) }
  }

  const have = new Set((data ?? []).map((p) => p.name.toLowerCase()))
  const quick = QUICK_PRODUCTS.filter((q) => !have.has(q.name.toLowerCase()))

  return (
    <div className="page narrow stack">
      <div className="spread">
        <h1 className="h1-sm">{t('prod.title')}</h1>
        {!editing && <button type="button" className="btn btn-primary btn-sm" onClick={() => setEditing({ ...EMPTY })}><Icon name="plus" size={16} /> {t('common.add')}</button>}
      </div>

      {editing && <ProductForm key={editing.id ?? 'new'} initial={editing} onSaved={() => { setEditing(null); reload() }} onCancel={() => setEditing(null)} />}

      {!editing && quick.length > 0 && (
        <div className="stack-sm">
          <p className="muted small">{t('prod.quick')}</p>
          <div className="chips">
            {quick.map((q) => (
              <button key={q.name} type="button" className="chip-btn" onClick={() => setEditing({ ...EMPTY, name: q.name, category: q.category, unit: q.unit })}>+ {pname(q.name)}</button>
            ))}
          </div>
        </div>
      )}

      {loading && !data && <ListSkeleton />}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data && data.length === 0 && !editing && <EmptyState icon="tag" title={t('prod.none')}>{t('prod.noneSub')}</EmptyState>}
      <ul className="plain stack-sm">
        {data?.map((p) => (
          <li key={p.id} className={`card product ${!p.available ? 'out' : ''}`}>
            <div className="grow">
              <h3>{pname(p.name)}</h3>
              <div className="muted small">{t(`cat.${p.category}`)} · {money(p.price)} / {p.unit} · {t('prod.inStock', { n: p.stock })}</div>
            </div>
            <button type="button" className={`switch sm ${p.available ? 'on' : ''}`} role="switch" aria-checked={p.available} aria-label={`${pname(p.name)} ${t('prod.available')}`} onClick={() => toggle(p)}><i aria-hidden="true" /></button>
            <button type="button" className="link" onClick={() => setEditing({ id: p.id, name: p.name, category: p.category, price: String(p.price), unit: p.unit, stock: String(p.stock) })}>{t('common.edit')}</button>
            <button type="button" className="icon-btn" onClick={() => remove(p)} aria-label={`${t('common.remove')} ${pname(p.name)}`}><Icon name="trash" size={18} /></button>
          </li>
        ))}
      </ul>
    </div>
  )
}
