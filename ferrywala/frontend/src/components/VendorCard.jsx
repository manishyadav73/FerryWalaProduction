import { Link } from 'react-router-dom'
import { useI18n } from '../i18n'
import Avatar from './Avatar'
import { StatusDot, Stars, Verified } from './Pills'
import Icon from './Icon'
import { useFavourites } from '../lib/favourites'

export default function VendorCard({ v }) {
  const { t, dist } = useI18n()
  const favs = useFavourites()
  const saved = favs.has(v.id)
  return (
    <div className="vc-wrap">
    <Link to={`/vendors/${v.id}`} className="card card-link vendor-card">
      <Avatar name={v.shopName} src={v.imageUrl} category={v.category} size={64} />
      <div className="vc-body">
        <div className="vc-title">
          <h3>{v.shopName}</h3>
          {v.verified && <Verified />}
        </div>
        <div className="muted small">{t(`cat.${v.category}`)}{v.area ? ` · ${v.area}` : ''}</div>
        <div className="vc-meta">
          <StatusDot online={v.online} />
          <Stars value={v.rating} count={v.ratingCount} />
          {v.distanceKm != null && <span className="small muted">{t('common.away', { d: dist(v.distanceKm) })}</span>}
        </div>
      </div>
    </Link>
    <button type="button" className={`fav ${saved ? 'on' : ''}`} aria-pressed={saved} aria-label={`${saved ? t('fav.remove') : t('fav.add')}: ${v.shopName}`} onClick={() => favs.toggle(v.id)}>
      <Icon name="heart" size={20} />
    </button>
    </div>
  )
}
