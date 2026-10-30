import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Reveal from '../components/Reveal'
import { RequestSteps } from '../components/RequestSteps'
import VendorCard from '../components/VendorCard'
import { renderApp } from './helpers.js'

afterEach(() => { vi.unstubAllGlobals(); localStorage.clear() })

describe('Reveal', () => {
  it('shows content immediately when IntersectionObserver is missing (never hides content)', () => {
    vi.stubGlobal('IntersectionObserver', undefined)
    render(<Reveal>hello</Reveal>)
    expect(screen.getByText('hello')).toHaveClass('in')
  })
  it('stays hidden until it intersects, then shows once', () => {
    let cb
    vi.stubGlobal('IntersectionObserver', class { constructor(f) { cb = f } observe() {} disconnect() {} })
    vi.stubGlobal('matchMedia', () => ({ matches: false, addEventListener() {}, removeEventListener() {} }))
    render(<Reveal>later</Reveal>)
    expect(screen.getByText('later')).not.toHaveClass('in')
    act(() => cb([{ isIntersecting: true }]))
    expect(screen.getByText('later')).toHaveClass('in')
  })
})

describe('RequestSteps', () => {
  it('marks done, current and upcoming steps', () => {
    renderApp(<RequestSteps step={1} />)
    const items = screen.getAllByRole('listitem')
    expect(items[0]).toHaveClass('done')
    expect(items[1]).toHaveClass('now')
    expect(items[1]).toHaveAttribute('aria-current', 'step')
    expect(items[2]).not.toHaveClass('done')
  })
})

describe('VendorCard favourite', () => {
  const v = { id: 5, shopName: 'Ravi Fruits', category: 'FRUITS', online: true, rating: 4, ratingCount: 1 }
  it('toggles saved state, announces it and persists on this device', async () => {
    renderApp(<VendorCard v={v} />)
    const btn = screen.getByRole('button', { name: /Save vendor on this device: Ravi Fruits/ })
    expect(btn).toHaveAttribute('aria-pressed', 'false')
    await userEvent.click(btn)
    expect(screen.getByRole('button', { name: /Remove saved vendor: Ravi Fruits/ })).toHaveAttribute('aria-pressed', 'true')
    expect(JSON.parse(localStorage.getItem('fw_favs'))).toContain(5)
  })
})
