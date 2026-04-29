import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import LoginPage from './LoginPage'
import { AuthProvider } from '../context/AuthContext'

const mockNavigate = vi.fn()
const mockGoogleLogin = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('@react-oauth/google', () => ({
  useGoogleLogin: (opts: { onSuccess: (r: { access_token: string }) => void }) => {
    mockGoogleLogin.mockImplementation(() => opts.onSuccess({ access_token: 'g-token' }))
    return mockGoogleLogin
  },
}))

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  googleAuth: vi.fn(),
}))

import * as authApi from '../api/auth'

function renderLogin() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renders the email and password fields and submit button', () => {
    renderLogin()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('shows validation errors for invalid input on submit', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.type(screen.getByLabelText('Password'), 'short')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText(/valid email/i)).toBeInTheDocument()
    expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument()
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('navigates to / on successful login', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      data: { token: 'jwt', email: 'alice@example.com', name: 'Alice' },
    } as Awaited<ReturnType<typeof authApi.login>>)

    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('Email'), 'alice@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'))
    expect(localStorage.getItem('token')).toBe('jwt')
  })

  it('shows error banner on bad credentials', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('bad'))

    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('Email'), 'alice@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText(/invalid email or password/i)).toBeInTheDocument()
    expect(mockNavigate).not.toHaveBeenCalled()
  })

  it('handles Google sign-in success', async () => {
    vi.mocked(authApi.googleAuth).mockResolvedValue({
      data: { token: 'g-jwt', email: 'alice@example.com', name: 'Alice' },
    } as Awaited<ReturnType<typeof authApi.googleAuth>>)

    const user = userEvent.setup()
    renderLogin()

    await user.click(screen.getByRole('button', { name: /google/i }))

    await waitFor(() => expect(authApi.googleAuth).toHaveBeenCalledWith('g-token'))
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'))
  })
})
