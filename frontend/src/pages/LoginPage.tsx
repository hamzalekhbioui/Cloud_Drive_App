import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login as loginApi } from '../api/auth'
import { useAuth } from '../context/AuthContext'
import Icon from '../components/Icon'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      const { data } = await loginApi(email, password)
      login(data.token, data.email, data.name)
      navigate('/')
    } catch {
      setError('Invalid email or password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-brand">
        <div className="brand"><span className="logo">V</span><span>Vault</span></div>
        <div className="pitch">
          <div className="display">Your team's files,<br /><em>under lock.</em></div>
          <p>Vault is end-to-end encrypted cloud storage built for teams who treat documents like assets — not attachments.</p>
        </div>
        <div className="foot">SOC 2 Type II · ISO 27001 · GDPR</div>
        <div className="orb" />
      </div>
      <div className="auth-form-wrap">
        <form className="auth-form" onSubmit={handleSubmit}>
          <h1>Welcome back.</h1>
          <div className="sub">Sign in to your workspace to continue.</div>

          {error && (
            <div style={{ padding: 10, marginBottom: 14, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 8, fontSize: 13 }}>
              {error}
            </div>
          )}

          <div className="field">
            <label>Work email</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@company.com" required />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" required />
          </div>
          <div style={{ textAlign: 'right', marginBottom: 12 }}>
            <a style={{ fontSize: 12, color: 'var(--ink-3)', textDecoration: 'underline', textUnderlineOffset: 3 }}>Forgot password?</a>
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
          <div className="or">or</div>
          <button type="button" className="btn btn-secondary" style={{ width: '100%', justifyContent: 'center', padding: '12px 16px' }}>
            <Icon name="shield" size={14} /> Continue with SSO
          </button>
          <div className="alt">
            Don't have an account? <Link to="/register">Create one</Link>
          </div>
        </form>
      </div>
    </div>
  )
}