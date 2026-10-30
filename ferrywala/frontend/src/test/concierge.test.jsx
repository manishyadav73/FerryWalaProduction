import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

vi.mock('../context/Auth', () => ({ useAuth: () => ({ user: { name: 'Asha Rao', role: 'CUSTOMER' } }) }))
vi.mock('../lib/api', () => ({
  api: {
    conciergeStatus: vi.fn(), conciergeHistory: vi.fn(), conciergeChat: vi.fn(), conciergeClear: vi.fn(),
    meetingUpdate: vi.fn(), sendMessage: vi.fn(), meeting: vi.fn(), createCase: vi.fn(),
  },
}))

import { api } from '../lib/api'
import Concierge from '../components/Concierge'
import { renderApp } from './helpers.js'

const reply = (over = {}) => ({
  reply: { text: 'Your request #7 is accepted.', cards: [], actions: [], suggestions: ['status'], verified: true, ...over },
  provider: 'rules',
})

beforeEach(() => {
  vi.clearAllMocks()
  api.conciergeStatus.mockResolvedValue({ name: 'AI Concierge', llmEnabled: false, retentionDays: 30, supportEmail: null, supportPhone: null })
  api.conciergeHistory.mockResolvedValue([])
})

describe('Concierge panel', () => {
  it('labels itself as an automated assistant, in rules mode when no model is configured', async () => {
    renderApp(<Concierge onClose={() => {}} />)
    expect(await screen.findByText(/Built-in rules/)).toBeInTheDocument()
    expect(screen.getByText(/AI assistant, not a person/)).toBeInTheDocument()
    expect(screen.getByText(/Chats are deleted after 30 days/)).toBeInTheDocument()
  })

  it('shows the server reply and marks it as from live data', async () => {
    api.conciergeChat.mockResolvedValue(reply())
    renderApp(<Concierge onClose={() => {}} orderId={7} />)
    await userEvent.type(screen.getByLabelText(/Ask about a request/), 'status?')
    await userEvent.click(screen.getByRole('button', { name: 'Send message' }))
    expect(await screen.findByText('Your request #7 is accepted.')).toBeInTheDocument()
    expect(screen.getByText(/From your live request data/)).toBeInTheDocument()
    expect(api.conciergeChat).toHaveBeenCalledWith(expect.objectContaining({ message: 'status?', orderId: 7 }))
  })

  it('never sends a proposed update without an explicit confirmation', async () => {
    api.conciergeChat.mockResolvedValue(reply({
      text: 'Want me to tell the vendor?',
      actions: [{ kind: 'SEND_MEETING_UPDATE', label: 'Tell vendor I am late', payload: { orderId: 7, type: 'RUNNING_LATE', etaMinutes: 10 } }],
    }))
    renderApp(<Concierge onClose={() => {}} />)
    await userEvent.type(screen.getByLabelText(/Ask about a request/), 'I am late')
    await userEvent.click(screen.getByRole('button', { name: 'Send message' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Tell vendor I am late' }))
    expect(api.meetingUpdate).not.toHaveBeenCalled()
    expect(screen.getByRole('dialog', { name: 'Please confirm' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Send' }))
    await waitFor(() => expect(api.meetingUpdate).toHaveBeenCalledWith(7, { type: 'RUNNING_LATE', etaMinutes: 10, source: 'CONCIERGE' }))
  })

  it('lets the user cancel a proposed update', async () => {
    api.conciergeChat.mockResolvedValue(reply({ actions: [{ kind: 'SEND_MEETING_UPDATE', label: 'Tell vendor', payload: { orderId: 7, type: 'ARRIVED' } }] }))
    renderApp(<Concierge onClose={() => {}} />)
    await userEvent.type(screen.getByLabelText(/Ask about a request/), 'hi')
    await userEvent.click(screen.getByRole('button', { name: 'Send message' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Tell vendor' }))
    await userEvent.click(screen.getByRole('button', { name: 'Not now' }))
    expect(api.meetingUpdate).not.toHaveBeenCalled()
  })

  it('shows an error with a retry that repeats the question', async () => {
    api.conciergeChat.mockRejectedValueOnce(Object.assign(new Error('x'), { code: 'NETWORK' })).mockResolvedValueOnce(reply())
    renderApp(<Concierge onClose={() => {}} />)
    await userEvent.type(screen.getByLabelText(/Ask about a request/), 'status')
    await userEvent.click(screen.getByRole('button', { name: 'Send message' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/Couldn't get an answer/)
    await userEvent.click(screen.getByRole('button', { name: /Try again/ }))
    expect(await screen.findByText('Your request #7 is accepted.')).toBeInTheDocument()
    expect(api.conciergeChat).toHaveBeenCalledTimes(2)
  })
})
