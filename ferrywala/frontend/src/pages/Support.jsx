import { useState } from 'react'
import CaseDialog from '../components/CaseDialog'
import { EmptyState, ErrorState, ListSkeleton } from '../components/States'
import { useI18n } from '../i18n'
import { api } from '../lib/api'
import { timeAgo } from '../lib/format'
import { useAsync } from '../lib/useAsync'
import { useTitle } from '../lib/useTitle'

export default function Support() {
  const { t } = useI18n()
  useTitle(t('support.title'))
  const { loading, error, data, reload } = useAsync((s) => api.myCases(s), [])
  const [open, setOpen] = useState(false)

  return (
    <div className="page narrow stack">
      <header className="row between">
        <div>
          <h1 className="h1-sm">{t('support.title')}</h1>
          <p className="muted">{t('support.sub')}</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setOpen(true)}>{t('support.new')}</button>
      </header>
      {loading && !data && <ListSkeleton />}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data?.length === 0 && <EmptyState icon="help" title={t('support.none')}>{t('support.noneSub')}</EmptyState>}
      {data?.map((c) => (
        <article key={c.id} className="card case-card">
          <header className="row between">
            <b>#{c.id} · {t(`case.cat.${c.category}`)}</b>
            <span className={`pill case-${c.status.toLowerCase()}`}>{t(`support.status.${c.status}`)}</span>
          </header>
          <p>{c.summary}</p>
          {c.resolutionNote && <p className="note"><b>{t('support.note')}:</b> {c.resolutionNote}</p>}
          <small className="muted">{timeAgo(c.createdAt)}{c.source === 'CONCIERGE' ? ` · ${t('support.fromConcierge')}` : ''}</small>
        </article>
      ))}
      {open && <CaseDialog onClose={() => setOpen(false)} onCreated={reload} />}
    </div>
  )
}
