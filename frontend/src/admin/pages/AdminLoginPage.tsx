import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import adminClient from '../api/adminClient'
import { useAdminAuth } from '../context/AdminAuthContext'

interface AdminLoginResponse {
  token: string
  email: string
  name: string
}

export default function AdminLoginPage() {
  const navigate = useNavigate()
  const { login } = useAdminAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const { data } = await adminClient.post<AdminLoginResponse>('/auth/login', { email, password })
      login(data.token, data.email, data.name)
      navigate('/admin', { replace: true })
    } catch {
      setError('Invalid email or password. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="gl-page">
      <div className="gl-main-side" style={{ width: '100%' }}>
        <div className="gl-card">
          <div className="gl-brand-row"><div className="gl-logo">V</div><span className="gl-wordmark">Vault Admin</span></div>
          <h1 className="gl-headline">Admin sign in</h1>
          <p className="gl-subhead">Sign in to manage Vault.</p>
          {error && <div className="gl-error-banner" role="alert">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: 20 }}>
              <label className="gl-label" htmlFor="admin-email">Email</label>
              <input className="gl-input" id="admin-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
            </div>
            <div style={{ marginBottom: 20 }}>
              <label className="gl-label" htmlFor="admin-password">Password</label>
              <input className="gl-input" id="admin-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
            </div>
            <button className="gl-btn-primary" type="submit" disabled={submitting}>{submitting ? 'Signing in…' : 'Sign in'}</button>
          </form>
        </div>
      </div>
    </main>
  )
}
