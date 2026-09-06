import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import PricingPage from './PricingPage'

vi.mock('../api/subscriptions', () => ({
  getSubscription: vi.fn(),
  getPlans: vi.fn(),
  createCheckoutSession: vi.fn(),
}))

import * as subscriptionsApi from '../api/subscriptions'

const subscription = {
  plan: 'FREE',
  status: 'ACTIVE',
  storageLimitBytes: 5_000,
  storageUsedBytes: 500,
  usagePercent: 10,
  startDate: '2026-01-01T00:00:00',
  endDate: null,
  billingInterval: 'MONTH',
  currentPeriodStart: null,
  currentPeriodEnd: null,
  cancelAtPeriodEnd: false,
} as const

const plans = [
  {
    name: 'Free', slug: 'FREE', storageLimitBytes: 5_000, maxFileSizeBytes: 100,
    maxTeams: 1, maxTeamMembers: 3, aiQueriesPerMonth: 10, rateLimitPerMinute: 100,
    priceCents: 0, currency: 'USD', billingInterval: 'MONTH',
  },
  {
    name: 'Pro', slug: 'PRO', storageLimitBytes: 50_000, maxFileSizeBytes: 1_000,
    maxTeams: 5, maxTeamMembers: 10, aiQueriesPerMonth: 200, rateLimitPerMinute: 500,
    priceCents: 999, currency: 'USD', billingInterval: 'MONTH',
  },
]

describe('PricingPage billing', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(subscriptionsApi.getSubscription).mockResolvedValue({ data: subscription } as never)
    vi.mocked(subscriptionsApi.getPlans).mockResolvedValue({ data: plans } as never)
  })

  it('creates checkout and redirects to the returned Stripe URL', async () => {
    vi.mocked(subscriptionsApi.createCheckoutSession).mockResolvedValue({
      data: { id: 'cs_test', url: 'https://checkout.stripe.test/session' },
    } as never)
    const assign = vi.fn()
    vi.stubGlobal('location', { assign })

    render(<MemoryRouter><PricingPage /></MemoryRouter>)
    await screen.findByText('Pro')
    await userEvent.click(screen.getByRole('button', { name: 'Choose plan' }))

    await waitFor(() => expect(subscriptionsApi.createCheckoutSession).toHaveBeenCalledWith('PRO'))
    expect(assign).toHaveBeenCalledWith('https://checkout.stripe.test/session')
    vi.unstubAllGlobals()
  })

  it('renders a useful checkout error without redirecting', async () => {
    vi.mocked(subscriptionsApi.createCheckoutSession).mockRejectedValue({
      isAxiosError: true,
      response: { status: 402, data: { message: 'Payment required' } },
    } as never)

    render(<MemoryRouter><PricingPage /></MemoryRouter>)
    await screen.findByText('Pro')
    await userEvent.click(screen.getByRole('button', { name: 'Choose plan' }))

    expect(await screen.findByText(/upgrade or payment action/i)).toBeInTheDocument()
  })

  it('treats a successful checkout return as informational', async () => {
    render(<MemoryRouter initialEntries={['/pricing?checkout=success']}><PricingPage /></MemoryRouter>)

    expect(await screen.findByText(/payment status will update after Stripe confirms it/i)).toBeInTheDocument()
    expect(subscriptionsApi.getSubscription).toHaveBeenCalled()
  })
})
