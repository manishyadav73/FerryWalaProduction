import { useI18n } from '../i18n'
import Icon from './Icon'

/** Progress of sending a request. `step` is 0-based: 0 items, 1 place, 2 send. Derived from real state. */
export function RequestSteps({ step }) {
  const { t } = useI18n()
  const labels = [t('steps.items'), t('steps.place'), t('steps.send')]
  return (
    <ol className="steps" aria-label={t('steps.label')}>
      {labels.map((l, i) => (
        <li key={l} className={i < step ? 'done' : i === step ? 'now' : ''} aria-current={i === step ? 'step' : undefined}>
          <span className="steps-dot">{i < step ? <Icon name="check" size={14} stroke={3} /> : i + 1}</span>
          <span>{l}</span>
        </li>
      ))}
    </ol>
  )
}
