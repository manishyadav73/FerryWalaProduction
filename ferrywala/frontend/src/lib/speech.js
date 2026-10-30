// Read-aloud and voice input using the browser's built-in speech support.
// Nothing is sent to FerryWala servers. Support varies (voice input is mainly Chrome/Android),
// so every caller must check `canSpeak` / `canListen` and keep a text fallback.

export const canSpeak = typeof window !== 'undefined' && 'speechSynthesis' in window

const Recognition = typeof window !== 'undefined' ? (window.SpeechRecognition || window.webkitSpeechRecognition) : null
export const canListen = !!Recognition

function pickVoice(lang) {
  const voices = window.speechSynthesis.getVoices()
  const base = lang.slice(0, 2)
  return voices.find((v) => v.lang === lang) || voices.find((v) => v.lang.startsWith(base)) || null
}

/** Returns false when this device has no voice for the language (so the UI can say so). */
export function speak(text, lang, { onEnd } = {}) {
  if (!canSpeak || !text) return false
  window.speechSynthesis.cancel()
  const u = new SpeechSynthesisUtterance(text)
  u.lang = lang
  const v = pickVoice(lang)
  if (v) u.voice = v
  else if (window.speechSynthesis.getVoices().length > 0 && lang !== 'en-IN') {
    onEnd?.()
    return false
  }
  u.rate = 0.92
  u.onend = () => onEnd?.()
  u.onerror = () => onEnd?.()
  window.speechSynthesis.speak(u)
  return true
}

export function stopSpeaking() {
  if (canSpeak) window.speechSynthesis.cancel()
}

/** One utterance -> text. Returns a stop() function. */
export function listen(lang, { onResult, onEnd, onError }) {
  if (!Recognition) return () => {}
  const r = new Recognition()
  r.lang = lang
  r.interimResults = false
  r.maxAlternatives = 1
  r.onresult = (e) => onResult?.(e.results[0][0].transcript)
  r.onerror = (e) => onError?.(e.error)
  r.onend = () => onEnd?.()
  try { r.start() } catch { onEnd?.() }
  return () => { try { r.stop() } catch { /* already stopped */ } }
}
