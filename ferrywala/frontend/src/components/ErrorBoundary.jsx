import { Component } from 'react'
import { useI18n } from '../i18n'

function Fallback() {
  const { t } = useI18n()
  return (
    <div className="center-screen">
      <div className="empty">
        <h2>{t('crash.title')}</h2>
        <p className="muted">{t('crash.body')}</p>
        <button type="button" className="btn btn-primary" onClick={() => window.location.assign('/')}>{t('crash.home')}</button>
      </div>
    </div>
  )
}

/** Last line of defence: a render crash shows a friendly screen instead of a white page. */
export default class ErrorBoundary extends Component {
  state = { failed: false }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  componentDidCatch(error) {
    console.error('Unhandled UI error', error)
  }

  render() {
    return this.state.failed ? <Fallback /> : this.props.children
  }
}
