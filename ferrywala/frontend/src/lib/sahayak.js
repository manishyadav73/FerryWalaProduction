// "Sahayak": a small, honest assistant. It does NOT use an LLM. It understands a fixed set of
// questions (typed, spoken, or tapped) and answers from the vendor's own data and the
// explainable demand engine, in the vendor's language.
import { buildSummary } from './advice.js'
import { navigateUrl } from './format.js'

export const INTENTS = ['where', 'carry', 'time', 'requests', 'online', 'photo', 'location', 'products', 'why', 'language']

// Words people actually type/say: English, Hinglish (Roman Hindi), Devanagari, Tamil, Tanglish.
const WORDS = {
  where: ['where', 'go today', 'which area', 'route', 'kahan', 'kaha', 'kidhar', 'jaun', 'jana', 'jaana', 'jaaun', 'कहाँ', 'कहां', 'कहा', 'जाऊं', 'जाऊँ', 'जाना', 'इलाक', 'एरिया', 'எங்கே', 'எங்க', 'போக', 'போகலாம்', 'எந்த இடம்', 'ஏரியா'],
  carry: ['carry', 'stock', 'what to sell', 'what should i bring', 'kya rakh', 'kya le', 'maal', 'सामान', 'क्या रख', 'क्या ले', 'माल', 'என்ன எடுத்து', 'என்ன கொண்டு', 'என்ன விற்க', 'சரக்கு'],
  time: ['time', 'when', 'kab', 'samay', 'kitne baje', 'टाइम', 'कब', 'समय', 'कितने बजे', 'நேரம்', 'எப்போது', 'எப்ப'],
  requests: ['request', 'order', 'orders', 'aaya', 'ऑर्डर', 'रिक्वेस्ट', 'अनुरोध', 'ஆர்டர்', 'கோரிக்கை', 'ரிக்வெஸ்ட்'],
  online: ['online', 'offline', 'go live', 'dukan', 'khol', 'band', 'ऑनलाइन', 'ऑफलाइन', 'दुकान', 'खोल', 'आन', 'ஆன்லைன்', 'ஆஃப்லைன்', 'கடை திற'],
  photo: ['photo', 'picture', 'image', 'tasveer', 'foto', 'फोटो', 'तस्वीर', 'फ़ोटो', 'படம்', 'புகைப்படம்', 'போட்டோ'],
  location: ['location', 'jagah', 'address', 'stall', 'लोकेशन', 'जगह', 'पता', 'இருப்பிடம்', 'லொகேஷன்', 'இடம் அமை'],
  products: ['add product', 'product', 'price', 'sabji add', 'daam', 'rate', 'प्रोडक्ट', 'सब्जी', 'सब्ज़ी', 'दाम', 'भाव', 'பொருள்', 'விலை', 'காய்கறி சேர்'],
  why: ['why', 'reason', 'kyun', 'kyu', 'kaise pata', 'क्यों', 'क्यूं', 'कारण', 'ஏன்', 'காரணம்'],
  language: ['language', 'bhasha', 'hindi', 'tamil', 'english', 'भाषा', 'हिंदी', 'हिन्दी', 'தமிழ்', 'மொழி', 'ஆங்கிலம்'],
}

// More specific intents win ties (checked first).
const ORDER = ['language', 'photo', 'products', 'location', 'online', 'requests', 'why', 'time', 'carry', 'where']

export function detectIntent(text) {
  const s = String(text || '').toLowerCase().trim()
  if (!s) return null
  for (const intent of ORDER) {
    if (WORDS[intent].some((w) => s.includes(w))) return intent
  }
  return null
}

/**
 * ctx: { t, pname, hour, dist, opp, me, stats, products }  (any data may be missing/loading)
 * returns { text, actions:[{label, to?, href?}] }
 */
export function answer(intent, ctx) {
  const { t, opp, me, stats } = ctx
  const best = opp?.best
  const go = best ? [{ label: t('sh.act.go'), href: navigateUrl(best.latitude, best.longitude) }] : []

  switch (intent) {
    case 'where':
      if (!me) return { text: t('sh.loading'), actions: [] }
      if (!Number.isFinite(me.latitude)) return { text: t('sh.noLocation'), actions: [{ label: t('sh.act.setLocation'), to: '/vendor/profile' }] }
      if (!best) return { text: t('sh.noOpp'), actions: [{ label: t('sh.act.products'), to: '/vendor/products' }] }
      return { text: buildSummary(best, ctx), actions: go }
    case 'carry':
      if (!best) return { text: t('sh.noOpp'), actions: [] }
      return {
        text: t('sh.carry', { list: best.topProducts.slice(0, 4).map((p) => ctx.pname(p.name)).join(', ') })
          + (best.topProducts.some((p) => !p.vendorCarries) ? ' ' + t('sh.carryAdd') : ''),
        actions: [{ label: t('sh.act.products'), to: '/vendor/products' }],
      }
    case 'time':
      if (!best) return { text: t('sh.noOpp'), actions: [] }
      return {
        text: t(best.recommendedWindow.fromData ? 'sh.timeData' : 'sh.timeTypical', { w: ctx.windowLabel(best.recommendedWindow), area: best.areaName }),
        actions: go,
      }
    case 'why':
      if (!best) return { text: t('sh.noOpp'), actions: [] }
      return { text: t('sh.why', { n: best.customerCount, s: best.signalCount }) + ' ' + t(`why.conf${best.confidence}`), actions: [] }
    case 'requests':
      return {
        text: stats ? (stats.pending > 0 ? t('sh.reqPending', { n: stats.pending }) : t('sh.reqNone')) : t('sh.reqHow'),
        actions: [{ label: t('sh.act.requests'), to: '/vendor/requests' }],
      }
    case 'online':
      return { text: t(me?.online ? 'sh.onlineYes' : 'sh.onlineNo'), actions: [{ label: t('sh.act.today'), to: '/vendor' }] }
    case 'photo':
      return { text: t('sh.photo'), actions: [{ label: t('sh.act.profile'), to: '/vendor/profile' }] }
    case 'location':
      return { text: t('sh.location'), actions: [{ label: t('sh.act.profile'), to: '/vendor/profile' }] }
    case 'products':
      return { text: t('sh.products'), actions: [{ label: t('sh.act.products'), to: '/vendor/products' }] }
    case 'language':
      return { text: t('sh.language'), actions: [] }
    default:
      return { text: t('sh.fallback'), actions: [] }
  }
}
