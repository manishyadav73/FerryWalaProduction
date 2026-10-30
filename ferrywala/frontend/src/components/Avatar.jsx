import { mediaUrl } from '../lib/api'
import { categoryColor } from '../lib/categories'
import { initials } from '../lib/format'

export default function Avatar({ name, src, category, size = 56, rounded = 18 }) {
  const url = mediaUrl(src)
  const style = { width: size, height: size, borderRadius: rounded }
  if (url) return <img className="avatar" style={style} src={url} alt={name ? `${name}` : ''} loading="lazy" decoding="async" width={size} height={size} />
  return (
    <span className="avatar avatar-fallback" style={{ ...style, background: categoryColor(category), fontSize: size * 0.36 }} aria-hidden="true">
      {initials(name)}
    </span>
  )
}
