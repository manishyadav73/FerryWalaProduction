import { useEffect, useRef, useState } from 'react'
import Avatar from '../../components/Avatar'
import Icon from '../../components/Icon'
import LocationPicker from '../../components/LocationPicker'
import { ErrorState, ListSkeleton } from '../../components/States'
import { useToast } from '../../context/Toast'
import { api } from '../../lib/api'
import { CATEGORIES } from '../../lib/categories'
import { useAsync } from '../../lib/useAsync'
import { useTitle } from '../../lib/useTitle'
import { useAuth } from '../../context/Auth'
import { useI18n } from '../../i18n'
import { Link, useNavigate } from 'react-router-dom'

const MAX_BYTES = 2 * 1024 * 1024
const TYPES = ['image/jpeg', 'image/png', 'image/webp']

function ImageUploader({ vendor, onChange }) {
  const toast = useToast()
  const { t, errMsg } = useI18n()
  const input = useRef(null)
  const [file, setFile] = useState(null)
  const [preview, setPreview] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!file) return undefined
    const url = URL.createObjectURL(file)
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  function pick(e) {
    const f = e.target.files?.[0]
    e.target.value = ''
    if (!f) return
    if (!TYPES.includes(f.type)) { setError(t('shop.badType')); return }
    if (f.size > MAX_BYTES) { setError(t('shop.tooBig')); return }
    setError('')
    setFile(f)
  }
  function discard() { setFile(null); setPreview(null) }

  async function upload() {
    setBusy(true)
    setError('')
    try {
      onChange(await api.vendorUploadImage(file))
      discard()
      toast.success(t('shop.saved'))
    } catch (e) {
      setError(errMsg(e))
    } finally {
      setBusy(false)
    }
  }
  async function remove() {
    setBusy(true)
    try { onChange(await api.vendorDeleteImage()); toast.info(t('shop.removed')) } catch (e) { setError(errMsg(e)) } finally { setBusy(false) }
  }

  return (
    <div className="card stack-sm uploader">
      <h3>{t('shop.photo')}</h3>
      <div className="row">
        {preview
          ? <img className="avatar" src={preview} width={96} height={96} style={{ width: 96, height: 96, borderRadius: 26 }} alt={t('shop.preview')} />
          : <Avatar name={vendor.shopName} src={vendor.imageUrl} category={vendor.category} size={96} rounded={26} />}
        <div className="stack-sm grow">
          <p className="muted small">{t('shop.photoHelp')}</p>
          <input ref={input} type="file" accept="image/jpeg,image/png,image/webp" className="sr-only" id="shop-photo" onChange={pick} />
          {!file && (
            <div className="row-gap">
              <label htmlFor="shop-photo" className="btn btn-ghost btn-sm"><Icon name="camera" size={16} /> {vendor.imageUrl ? t('shop.replace') : t('shop.choose')}</label>
              {vendor.imageUrl && <button type="button" className="btn btn-danger btn-sm" onClick={remove} disabled={busy}>{t('common.remove')}</button>}
            </div>
          )}
          {file && (
            <div className="row-gap">
              <button type="button" className="btn btn-primary btn-sm" onClick={upload} disabled={busy}>{busy ? t('shop.uploading') : t('shop.savePhoto')}</button>
              <button type="button" className="btn btn-ghost btn-sm" onClick={discard} disabled={busy}>{t('common.cancel')}</button>
            </div>
          )}
        </div>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
    </div>
  )
}

function Details({ vendor, onChange }) {
  const toast = useToast()
  const { t } = useI18n()
  const [f, setF] = useState({ shopName: vendor.shopName, category: vendor.category, description: vendor.description ?? '', address: vendor.address ?? '', area: vendor.area ?? '' })
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState({})
  const set = (k) => (e) => setF((x) => ({ ...x, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    if (f.shopName.trim().length < 2) { setErrors({ shopName: t('shop.vName') }); return }
    setBusy(true)
    setErrors({})
    try {
      onChange(await api.vendorUpdate({ ...f, shopName: f.shopName.trim() }))
      toast.success(t('shop.detailsSaved'))
    } catch (err) {
      setErrors(err.details && Object.keys(err.details).length ? err.details : { form: t('err.generic') })
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card stack-sm" onSubmit={submit} noValidate>
      <h3>{t('shop.details')}</h3>
      {errors.form && <p className="form-error" role="alert">{errors.form}</p>}
      <label className="field"><span>{t('auth.shopName')}</span><input className="input" value={f.shopName} onChange={set('shopName')} maxLength={80} aria-invalid={!!errors.shopName} />{errors.shopName && <em className="field-error">{errors.shopName}</em>}</label>
      <fieldset className="field"><legend>{t('prod.category')}</legend>
        <div className="chips">{CATEGORIES.map((c) => <button key={c.value} type="button" aria-pressed={f.category === c.value} className={`chip-btn ${f.category === c.value ? 'on' : ''}`} onClick={() => setF((x) => ({ ...x, category: c.value }))}>{t(`cat.${c.value}`)}</button>)}</div>
      </fieldset>
      <label className="field"><span>{t('shop.about')}</span><textarea className="input" value={f.description} onChange={set('description')} maxLength={500} placeholder={t('shop.aboutPlaceholder')} />{errors.description && <em className="field-error">{errors.description}</em>}</label>
      <label className="field"><span>{t('shop.area')}</span><input className="input" value={f.area} onChange={set('area')} maxLength={80} /></label>
      <label className="field"><span>{t('shop.address')}</span><input className="input" value={f.address} onChange={set('address')} maxLength={200} /></label>
      <button className="btn btn-primary" disabled={busy}>{busy ? t('common.saving') : t('shop.saveDetails')}</button>
    </form>
  )
}

function Place({ vendor, onChange }) {
  const toast = useToast()
  const { t, errMsg } = useI18n()
  const [pick, setPick] = useState(null)
  const [busy, setBusy] = useState(false)
  async function save() {
    setBusy(true)
    try {
      onChange(await api.vendorLocation({ latitude: pick.lat, longitude: pick.lng, area: pick.label?.split(',')[0]?.slice(0, 80), address: pick.label?.slice(0, 200) }))
      setPick(null)
      toast.success(t('shop.locSaved'))
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="card stack-sm">
      <h3>{t('shop.loc')}</h3>
      <p className="muted small">{Number.isFinite(vendor.latitude) ? <>{t('shop.locCurrent')}: <b>{vendor.area || vendor.address || t('shop.locSavedWord')}</b>. {t('shop.locUpdate')}</> : t('shop.locNone')}</p>
      <LocationPicker value={pick} onChange={setPick} />
      {pick && <button type="button" className="btn btn-primary" onClick={save} disabled={busy}>{busy ? t('common.saving') : t('shop.locUse')}</button>}
    </div>
  )
}

export default function Shop() {
  const { t } = useI18n()
  useTitle(t('shop.title'))
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const me = useAsync((s) => api.vendorMe(s), [])
  if (me.loading && !me.data) return <div className="page narrow"><ListSkeleton /></div>
  if (me.error) return <div className="page narrow"><ErrorState error={me.error} onRetry={me.reload} /></div>
  const v = me.data
  return (
    <div className="page narrow stack">
      <div className="spread"><h1 className="h1-sm">{t('shop.title')}</h1><Link className="link" to={`/vendors/${v.id}`}>{t('shop.preview2')}</Link></div>
      <ImageUploader vendor={v} onChange={me.setData} />
      <Details vendor={v} onChange={me.setData} />
      <Place vendor={v} onChange={me.setData} />
      <div className="card stack-sm">
        <div className="muted small">{t('shop.signedInAs', { email: user.email })}</div>
        <button type="button" className="btn btn-ghost btn-block" onClick={() => { logout(); nav('/') }}><Icon name="logout" size={20} /> {t('common.signOut')}</button>
      </div>
    </div>
  )
}
