import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import SettingsPage from './SettingsPage'
import { AuthProvider } from '../context/AuthContext'
import { ThemeProvider } from '../context/ThemeContext'

vi.mock('../api/settings', () => ({
  getSettings: vi.fn(),
  updatePreferences: vi.fn(),
  updateProfile: vi.fn(),
  updatePassword: vi.fn(),
  regenerateApiToken: vi.fn(),
}))
vi.mock('../api/subscriptions', () => ({
  getSubscription: vi.fn(),
  getUsage: vi.fn(),
  cancelSubscription: vi.fn(),
  reactivateSubscription: vi.fn(),
  createPortalSession: vi.fn(),
}))

import * as settingsApi from '../api/settings'
import * as subscriptionsApi from '../api/subscriptions'

describe('Settings billing', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(settingsApi.getSettings).mockResolvedValue({
      data: {
        name: 'Alice', email: 'alice@example.com', darkMode: false, density: 'comfortable',
        emailNotifications: true, pushNotifications: true, language: 'en',
        autoOrganize: false, showFileExtensions: true, defaultView: 'grid',
        apiToken: null,
      },
    } as never)
    vi.mocked(subscriptionsApi.getSubscription).mockResolvedValue({
      data: {
        plan: 'PRO', status: 'PAST_DUE', storageLimitBytes: 50_000, storageUsedBytes: 10_000,
        usagePercent: 20, startDate: '2026-01-01T00:00:00', endDate: null,
        billingInterval: 'MONTH', currentPeriodStart: '2026-09-01T00:00:00',
        currentPeriodEnd: '2026-10-01T00:00:00', cancelAtPeriodEnd: true,
      },
    } as never)
    vi.mocked(subscriptionsApi.getUsage).mockResolvedValue({
      data: {
        storageLimitBytes: 50_000, storageUsedBytes: 10_000, usagePercent: 20,
        aiQueriesUsed: 4, aiQueriesLimit: 200, periodStart: '2026-09-01', periodEnd: '2026-09-30',
      },
    } as never)
  })

  it('renders past-due state and billing usage', async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <ThemeProvider><SettingsPage /></ThemeProvider>
        </AuthProvider>
      </MemoryRouter>,
    )
    await userEvent.click(await screen.findByRole('button', { name: 'Billing' }))

    expect(await screen.findByText(/latest payment failed/i)).toBeInTheDocument()
    expect(screen.getByText(/PRO · PAST DUE/i)).toBeInTheDocument()
    expect(screen.getByText(/10 KB of 49 KB used/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reactivate' })).toBeInTheDocument()
  })
})
