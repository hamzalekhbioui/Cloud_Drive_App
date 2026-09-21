import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import AdminLoginPage from './AdminLoginPage'
import AdminRoute from '../components/AdminRoute'
import AdminShell from '../components/AdminShell'
import { AdminAuthProvider } from '../context/AdminAuthContext'
import adminClient from '../api/adminClient'
import { ThemeProvider } from '../../context/ThemeContext'

vi.mock('../api/adminClient', () => ({
  default: { post: vi.fn() },
}))

function renderLogin() {
  return render(
    <ThemeProvider>
      <AdminAuthProvider>
        <MemoryRouter initialEntries={['/admin/login']}>
          <Routes>
            <Route path="/admin/login" element={<AdminLoginPage />} />
            <Route path="/admin" element={<div>Admin home</div>} />
          </Routes>
        </MemoryRouter>
      </AdminAuthProvider>
    </ThemeProvider>,
  )
}

describe('AdminLoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('logs in successfully and redirects to the admin shell', async () => {
    vi.mocked(adminClient.post).mockResolvedValue({
      data: { token: 'admin-jwt', email: 'admin@example.com', name: 'Admin' },
    })
    const user = userEvent.setup()
    renderLogin()

    await user.type(screen.getByLabelText('Email'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => expect(screen.getByText('Admin home')).toBeInTheDocument())
    expect(localStorage.getItem('admin_token')).toBe('admin-jwt')
  })

  it('redirects unauthenticated users away from the admin shell', () => {
    render(
      <ThemeProvider>
        <AdminAuthProvider>
          <MemoryRouter initialEntries={['/admin']}>
            <Routes>
              <Route path="/admin/login" element={<div>Admin login</div>} />
              <Route path="/admin" element={<AdminRoute><AdminShell /></AdminRoute>} />
            </Routes>
          </MemoryRouter>
        </AdminAuthProvider>
      </ThemeProvider>,
    )

    expect(screen.getByText('Admin login')).toBeInTheDocument()
  })
})
