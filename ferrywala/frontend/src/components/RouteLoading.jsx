import { ListSkeleton } from './States'

/** Suspense fallback while a page's code downloads: a slim progress bar over a skeleton. */
export default function RouteLoading() {
  return (
    <div className="page" aria-busy="true">
      <div className="route-bar" role="progressbar" aria-label="Loading"><i /></div>
      <ListSkeleton />
    </div>
  )
}
