import { lazy, Suspense } from 'react'
import { Skeleton } from './States'

const MapView = lazy(() => import('./MapView'))

/** Leaflet (~150 KB) only downloads when a map is actually shown. */
export default function LazyMap({ height = 320, ...props }) {
  return (
    <Suspense fallback={<Skeleton h={height} r={20} />}>
      <MapView height={height} {...props} />
    </Suspense>
  )
}
