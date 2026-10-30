import { beforeEach, describe, expect, it, vi } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

vi.mock('../components/LazyMap', () => ({ default: () => <div data-testid="map" /> }))
vi.mock('../lib/api', () => ({
  api: { meeting: vi.fn(), startShare: vi.fn(), stopShare: vi.fn(), meetingUpdate: vi.fn(), sendMessage: vi.fn() },
}))

import { api } from '../lib/api'
import Meet from '../pages/Meet'
import { renderApp } from './helpers.js'

const base = {
  orderId: 7, status: 'ACCEPTED', coordinationOpen: true, messagingOpen: true, viewerRole: 'CUSTOMER',
  otherName: 'Ravi', shopName: 'Ravi Fruits', latitude: 12.9, longitude: 77.6, areaName: 'Park Gate', note: null,
  myShare: { active: false, expiresAt: null }, otherShare: { active: false }, distanceMeters: null, updates: [], messages: [],
}
const show = () => renderApp(<Routes><Route path="/meet/:id" element={<Meet />} /></Routes>, { route: '/meet/7' })

beforeEach(() => {
  vi.clearAllMocks()
  api.meeting.mockResolvedValue(base)
  api.startShare.mockResolvedValue({})
})

describe('Meet page', () => {
  it('works without location: shows the pinned spot, directions and a not-sharing state', async () => {
    show()
    expect(await screen.findByText(/Meeting point: Park Gate/)).toBeInTheDocument()
    expect(screen.getByText(/You are not sharing your location/)).toBeInTheDocument()
    expect(screen.getByText(/Ravi is not sharing their location/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Open directions/ })).toHaveAttribute('href', expect.stringContaining('12.9'))
  })

  it('asks for consent first and does not touch geolocation or the API until confirmed', async () => {
    const getCurrentPosition = vi.fn()
    Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition, watchPosition: vi.fn(), clearWatch: vi.fn() }, configurable: true })
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Start sharing' }))
    expect(screen.getByRole('dialog', { name: /Share your location with Ravi/ })).toBeInTheDocument()
    expect(getCurrentPosition).not.toHaveBeenCalled()
    expect(api.startShare).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Not now' }))
    expect(getCurrentPosition).not.toHaveBeenCalled()
    expect(api.startShare).not.toHaveBeenCalled()
  })

  it('sends the chosen duration after consent and stays usable when permission is denied', async () => {
    const getCurrentPosition = vi.fn((ok, no) => no({ code: 1 }))
    Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition, watchPosition: vi.fn(), clearWatch: vi.fn() }, configurable: true })
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Start sharing' }))
    await userEvent.click(within_dialog('Start sharing'))
    await waitFor(() => expect(getCurrentPosition).toHaveBeenCalled())
    expect(api.startShare).not.toHaveBeenCalled()
    expect(await screen.findByText(/Location permission was denied/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Start sharing' })).toBeInTheDocument()
  })

  it('shows meeting tools closed while the request is still pending', async () => {
    api.meeting.mockResolvedValue({ ...base, status: 'PENDING', coordinationOpen: false })
    show()
    expect(await screen.findByText(/Meeting tools open once the vendor accepts/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Start sharing' })).not.toBeInTheDocument()
  })

  it('treats an unavailable request as a plain not-found message', async () => {
    api.meeting.mockRejectedValue(Object.assign(new Error('nf'), { status: 404 }))
    show()
    expect(await screen.findByText(/not available to you/)).toBeInTheDocument()
  })
})

function within_dialog(name) {
  const dlg = screen.getByRole('dialog')
  return [...dlg.querySelectorAll('button')].find((b) => b.textContent === name)
}
