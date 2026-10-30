import { describe, expect, it } from 'vitest'
import en from '../i18n/en.js'
import hi from '../i18n/hi.js'
import ta from '../i18n/ta.js'

const placeholders = (s) => [...String(s).matchAll(/\{(\w+)\}/g)].map((m) => m[1])

describe('i18n dictionaries', () => {
  it('Hindi and Tamil have every English key', () => {
    const missing = (d) => Object.keys(en).filter((k) => !(k in d))
    expect(missing(hi)).toEqual([])
    expect(missing(ta)).toEqual([])
  })
  it('translations keep every {placeholder} English uses (may add more, e.g. time.hour)', () => {
    const bad = []
    for (const [name, d] of [['hi', hi], ['ta', ta]]) {
      for (const k of Object.keys(en)) if (k in d && !placeholders(en[k]).every((p) => placeholders(d[k]).includes(p))) bad.push(`${name}:${k}`)
    }
    expect(bad).toEqual([])
  })
})
