import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useGoogleLogin } from '@react-oauth/google'
import { motion, AnimatePresence } from 'framer-motion'
import { login as loginApi, googleAuth } from '../api/auth'
import { useAuth } from '../context/AuthContext'

// ── Validation ────────────────────────────────────────────────────────────────
const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1]

function validateEmail(v: string): string | null {
  if (!emailRe.test(v)) return 'Enter a valid email address.'
  return null
}
function validatePassword(v: string): string | null {
  if (v.length < 8) return 'Password must be at least 8 characters.'
  return null
}

// ── Animation variants ────────────────────────────────────────────────────────
const cardVariants = {
  hidden: { opacity: 0, y: 18, scale: 0.985 },
  show: { opacity: 1, y: 0, scale: 1, transition: { duration: 0.7, ease: EASE } },
}
const containerVariants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.08, delayChildren: 0.15 } },
}
const itemVariants = {
  hidden: { opacity: 0, y: 14, filter: 'blur(6px)' },
  show: { opacity: 1, y: 0, filter: 'blur(0px)', transition: { duration: 0.55, ease: EASE } },
}

// ── Icons ─────────────────────────────────────────────────────────────────────
function AlertIcon({ size = 14 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
      <path d="M12 7v6M12 17h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

function EyeOnIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.94 10.94 0 0 1 12 20c-7 0-11-8-11-8a18.4 18.4 0 0 1 5.06-5.94" />
      <path d="M9.9 4.24A10.9 10.9 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
      <path d="M14.12 14.12A3 3 0 0 1 9.88 9.88" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  )
}

function SpinnerIcon() {
  return (
    <svg className="gl-spin" width="14" height="14" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="9" stroke="rgba(26,11,46,0.25)" strokeWidth="3" />
      <path d="M12 3a9 9 0 0 1 9 9" stroke="#1a0b2e" strokeWidth="3" strokeLinecap="round" />
    </svg>
  )
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24">
      <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
      <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
      <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05" />
      <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
    </svg>
  )
}

function GithubIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 .5C5.6.5.5 5.6.5 12c0 5.1 3.3 9.4 7.9 10.9.6.1.8-.3.8-.6v-2.1c-3.2.7-3.9-1.4-3.9-1.4-.5-1.3-1.3-1.7-1.3-1.7-1.1-.7.1-.7.1-.7 1.2.1 1.8 1.2 1.8 1.2 1.1 1.8 2.8 1.3 3.4 1 .1-.8.4-1.3.8-1.6-2.6-.3-5.3-1.3-5.3-5.7 0-1.3.5-2.3 1.2-3.1-.1-.3-.5-1.5.1-3.1 0 0 1-.3 3.3 1.2 1-.3 2-.4 3-.4s2 .1 3 .4c2.3-1.5 3.3-1.2 3.3-1.2.7 1.6.2 2.8.1 3.1.8.8 1.2 1.9 1.2 3.1 0 4.4-2.7 5.4-5.3 5.7.4.4.8 1.1.8 2.2v3.3c0 .3.2.7.8.6 4.6-1.5 7.9-5.8 7.9-10.9C23.5 5.6 18.4.5 12 .5z" />
    </svg>
  )
}

// ── Floating blobs ────────────────────────────────────────────────────────────
interface BlobProps {
  style: React.CSSProperties
  animate: { x: number; y: number }
  duration: number
}

function Blob({ style, animate, duration }: BlobProps) {
  return (
    <motion.div
      className="gl-blob"
      style={style}
      animate={animate}
      transition={{ duration, repeat: Infinity, repeatType: 'mirror', ease: 'easeInOut' }}
    />
  )
}

function Blobs() {
  return (
    <div className="gl-blobs">
      <Blob
        style={{ width: 520, height: 520, left: '-8%', top: '-10%',
          background: 'radial-gradient(circle, #a855f7 0%, transparent 60%)' }}
        animate={{ x: 40, y: 30 }}
        duration={9}
      />
      <Blob
        style={{ width: 460, height: 460, right: '-6%', top: '8%',
          background: 'radial-gradient(circle, #ec4899 0%, transparent 60%)' }}
        animate={{ x: -30, y: 20 }}
        duration={11}
      />
      <Blob
        style={{ width: 560, height: 560, left: '30%', bottom: '-20%',
          background: 'radial-gradient(circle, #38bdf8 0%, transparent 60%)' }}
        animate={{ x: 30, y: -20 }}
        duration={13}
      />
    </div>
  )
}

// ── Inline field error ────────────────────────────────────────────────────────
function FieldError({ msg }: { msg: string | null }) {
  return (
    <AnimatePresence>
      {msg && (
        <motion.div
          initial={{ opacity: 0, y: -4, height: 0 }}
          animate={{ opacity: 1, y: 0, height: 'auto' }}
          exit={{ opacity: 0, y: -4, height: 0 }}
          transition={{ duration: 0.18 }}
          style={{ overflow: 'hidden' }}
        >
          <div className="gl-field-error">
            <AlertIcon size={13} /> {msg}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [remember, setRemember] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [touched, setTouched] = useState({ email: false, password: false })
  const [googleLoading, setGoogleLoading] = useState(false)

  const emailErr = touched.email ? validateEmail(email) : null
  const passwordErr = touched.password ? validatePassword(password) : null
  const emailOk = email.length > 0 && !validateEmail(email)
  const passwordOk = password.length >= 8
  const formValid = emailOk && passwordOk
  const isLoading = submitting || googleLoading

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setTouched({ email: true, password: true })
    setSubmitError('')
    if (!formValid) return
    setSubmitting(true)
    try {
      const { data } = await loginApi(email, password)
      login(data.token, data.email, data.name)
      navigate('/')
    } catch {
      setSubmitError('Invalid email or password. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleGoogle = useGoogleLogin({
    onSuccess: async (tokenResponse) => {
      setGoogleLoading(true)
      setSubmitError('')
      try {
        const { data } = await googleAuth(tokenResponse.access_token)
        login(data.token, data.email, data.name)
        navigate('/')
      } catch {
        setSubmitError('Google sign-in failed. Please try again.')
      } finally {
        setGoogleLoading(false)
      }
    },
    onError: () => setSubmitError('Google sign-in failed. Please try again.'),
  })

  return (
    <>
      {/* Fixed background layers */}
      <div className="gl-bg" />
      <div className="gl-bg-radial" />
      <div className="gl-grain" />
      <Blobs />

      {/* Scrollable page — centers the card */}
      <div className="gl-page">
        <motion.div
          className="gl-card"
          variants={cardVariants}
          initial="hidden"
          animate="show"
        >
          <motion.div variants={containerVariants} initial="hidden" animate="show">

            {/* Brand row */}
            <motion.div variants={itemVariants} className="gl-brand-row">
              <div className="gl-logo">
                <img src="/cloud_drive_app.png" alt="Vault" width="24" height="24" style={{ objectFit: 'contain' }} />
              </div>
              <span className="gl-wordmark">Vault</span>
            </motion.div>

            {/* Headline */}
            <motion.h1 variants={itemVariants} className="gl-headline">
              Welcome back.
            </motion.h1>
            <motion.p variants={itemVariants} className="gl-subhead">
              Sign in to your account to continue.
            </motion.p>

            {/* Submit error banner */}
            <AnimatePresence>
              {submitError && (
                <motion.div
                  initial={{ opacity: 0, y: -8, height: 0 }}
                  animate={{ opacity: 1, y: 0, height: 'auto' }}
                  exit={{ opacity: 0, y: -8, height: 0 }}
                  transition={{ duration: 0.22 }}
                  style={{ overflow: 'hidden' }}
                >
                  <div className="gl-error-banner">
                    <AlertIcon size={14} /> {submitError}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            <form onSubmit={handleSubmit} noValidate>

              {/* Email */}
              <motion.div variants={itemVariants} style={{ marginBottom: 20 }}>
                <label className="gl-label" htmlFor="gl-email">Email</label>
                <input
                  id="gl-email"
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={e => { setEmail(e.target.value); setTouched(t => ({ ...t, email: true })) }}
                  placeholder="you@studio.com"
                  className={`gl-input${emailErr ? ' err' : ''}${emailOk && touched.email ? ' ok' : ''}`}
                />
                <FieldError msg={emailErr} />
              </motion.div>

              {/* Password */}
              <motion.div variants={itemVariants} style={{ marginBottom: 20 }}>
                <div className="gl-field-head">
                  <label className="gl-label" htmlFor="gl-password">Password</label>
                </div>
                <div style={{ position: 'relative' }}>
                  <input
                    id="gl-password"
                    type={showPw ? 'text' : 'password'}
                    autoComplete="current-password"
                    value={password}
                    onChange={e => { setPassword(e.target.value); setTouched(t => ({ ...t, password: true })) }}
                    placeholder="••••••••"
                    className={`gl-input gl-input-pw${passwordErr ? ' err' : ''}${passwordOk && touched.password ? ' ok' : ''}`}
                  />
                  <button
                    type="button"
                    className="gl-eye-btn"
                    onClick={() => setShowPw(s => !s)}
                    aria-label={showPw ? 'Hide password' : 'Show password'}
                  >
                    {showPw ? <EyeOffIcon /> : <EyeOnIcon />}
                  </button>
                </div>
                <FieldError msg={passwordErr} />
              </motion.div>

              {/* Remember me */}
              <motion.div variants={itemVariants} className="gl-remember">
                <label className="gl-remember-label">
                  <input
                    type="checkbox"
                    className="gl-checkbox"
                    checked={remember}
                    onChange={e => setRemember(e.target.checked)}
                  />
                  Remember me for 30 days
                </label>
              </motion.div>

              {/* Submit button */}
              <motion.div variants={itemVariants}>
                <button type="submit" className="gl-btn-primary" disabled={isLoading}>
                  {submitting ? <><SpinnerIcon /> Signing in…</> : 'Sign in'}
                </button>
              </motion.div>

              {/* Divider */}
              <motion.div variants={itemVariants} className="gl-divider">
                <div className="gl-divider-line" />
                <span className="gl-divider-label">or continue with</span>
                <div className="gl-divider-line" />
              </motion.div>

              {/* Social buttons */}
              <motion.div variants={itemVariants} className="gl-social-grid">
                <button
                  type="button"
                  className="gl-btn-social"
                  onClick={() => handleGoogle()}
                  disabled={isLoading}
                >
                  <GoogleIcon /> {googleLoading ? 'Signing in…' : 'Google'}
                </button>
                <button
                  type="button"
                  className="gl-btn-social"
                  disabled
                  title="GitHub sign-in coming soon"
                >
                  <GithubIcon /> GitHub
                </button>
              </motion.div>

              {/* Footer */}
              <motion.p variants={itemVariants} className="gl-footer">
                New here?{' '}
                <Link to="/register" className="gl-footer-link">Create an account</Link>
              </motion.p>

            </form>
          </motion.div>
        </motion.div>
      </div>
    </>
  )
}