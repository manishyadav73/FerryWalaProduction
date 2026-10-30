import { useI18n } from '../i18n'

/** Shown once, to first-time visitors. Big buttons, each in its own script, no reading needed. */
export default function LanguagePicker() {
  const { chosen, setLang, languages, lang } = useI18n()
  if (chosen) return null
  return (
    <div className="picker-backdrop">
      <div className="picker-sheet" role="dialog" aria-modal="true" aria-labelledby="lp-title">
        <h2 id="lp-title">अपनी भाषा चुनें<br />உங்கள் மொழியைத் தேர்ந்தெடுங்கள்<br />Choose your language</h2>
        <div className="lp-grid">
          {languages.map((l) => (
            <button key={l.code} type="button" className={`lp-btn ${l.code === lang ? 'suggested' : ''}`} onClick={() => setLang(l.code)}>
              <span className="lp-native">{l.native}</span>
              {l.code !== 'en' && <span className="lp-sub">{l.label}</span>}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
