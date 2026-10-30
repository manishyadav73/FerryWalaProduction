import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Dialog from '../components/Dialog'
import { renderApp } from './helpers.js'

describe('Dialog', () => {
  it('is a labelled modal, closes on Escape and moves focus inside', async () => {
    const onClose = vi.fn()
    renderApp(<Dialog title="Please confirm" onClose={onClose} footer={<button>OK</button>}><p>Body</p></Dialog>)
    const dlg = screen.getByRole('dialog', { name: 'Please confirm' })
    expect(dlg).toHaveAttribute('aria-modal', 'true')
    expect(dlg.contains(document.activeElement)).toBe(true)
    await userEvent.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledTimes(1)
  })
  it('does not close while busy', async () => {
    const onClose = vi.fn()
    renderApp(<Dialog title="t" onClose={onClose} busy><p>x</p></Dialog>)
    await userEvent.keyboard('{Escape}')
    expect(onClose).not.toHaveBeenCalled()
  })
})
