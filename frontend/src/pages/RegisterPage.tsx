import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register as registerApi } from '../api/auth'
import { useAuth } from '../context/AuthContext'
import Icon from '../components/Icon'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      const { data } = await registerApi(name, email, password)
      login(data.token, data.email, data.name)
      navigate('/')
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Registration failed. Try a different email.'
      setError(msg)
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
          <p>Start your 14-day Business trial. No card required, cancel anytime.</p>
        </div>
        <div className="foot">SOC 2 Type II · ISO 27001 · GDPR</div>
        <div className="orb" />
      </div>
      <div className="auth-form-wrap">
        <form className="auth-form" onSubmit={handleSubmit}>
          <h1>Create an account.</h1>
          <div className="sub">Start your 14-day Business trial.</div>

          {error && (
            <div style={{ padding: 10, marginBottom: 14, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 8, fontSize: 13 }}>
              {error}
            </div>
          )}

          <div className="field">
            <label>Full name</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="Alex Morgan" required />
          </div>
          <div className="field">
            <label>Work email</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@company.com" required />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Min. 6 characters" minLength={6} required />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Creating account…' : 'Create account'}
          </button>
          <div className="or">or</div>
          <button type="button" className="btn btn-secondary" style={{ width: '100%', justifyContent: 'center', padding: '12px 16px' }}>
            <Icon name="shield" size={14} /> Continue with SSO
          </button>
          <div className="alt">
            Already have one? <Link to="/login">Sign in</Link>
          </div>
        </form>
      </div>
    </div>
  )
}