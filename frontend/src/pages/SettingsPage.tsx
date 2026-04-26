import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  getSettings, updatePreferences, updateProfile,
  updatePassword, regenerateApiToken,
} from '../api/settings'
import type { SettingsData } from '../api/settings'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import ToggleSwitch from '../components/settings/ToggleSwitch'
import Toast from '../components/settings/Toast'
import type { ToastState } from '../components/settings/Toast'
import { formatBytes } from '../utils/files'

// ═══════════════════════════════════════════════════════════════════════════
// Tiny inline SVG icons  (must be defined before SECTIONS)
// ═══════════════════════════════════════════════════════════════════════════
const i = (d: string) => () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d={d} />
  </svg>
)
const ProfileIcon  = i('M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z')
const LockIcon     = i('M17 11V8a5 5 0 0 0-10 0v3M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z')
const PaletteIcon  = i('M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10a2 2 0 0 0 2-2c0-.5-.2-1-.5-1.4-.3-.4-.5-.8-.5-1.3 0-1.1.9-2 2-2h2.4C19.9 15.3 22 13 22 10c0-4.4-4.5-8-10-8z')
const StorageIcon  = i('M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z')
const BellIcon     = i('M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0')
const SlidersIcon  = i('M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6')
const TerminalIcon = i('M4 17l6-6-6-6M12 19h8')
const GridIcon     = () => <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
const ListIcon     = () => <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
const CopyIcon     = () => <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
const CheckIcon    = () => <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><path d="m5 12 5 5 9-11"/></svg>
const InfoIcon     = () => <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>

// ── Sidebar sections ─────────────────────────────────────────────────────────
const SECTIONS = [
  { id: 'profile',       label: 'Profile',       icon: ProfileIcon },
  { id: 'security',      label: 'Security',       icon: LockIcon },
  { id: 'appearance',    label: 'Appearance',     icon: PaletteIcon },
  { id: 'storage',       label: 'Storage',        icon: StorageIcon },
  { id: 'notifications', label: 'Notifications',  icon: BellIcon },
  { id: 'preferences',   label: 'Preferences',    icon: SlidersIcon },
  { id: 'advanced',      label: 'Advanced',       icon: TerminalIcon },
]

// ── Page ─────────────────────────────────────────────────────────────────────
export default function SettingsPage() {
  const { login } = useAuth()
  const { theme, setTheme, density, setDensity } = useTheme()

  const [data,    setData]    = useState<SettingsData | null>(null)
  const [loading, setLoading] = useState(true)
  const [section, setSection] = useState('profile')
  const [toast,   setToast]   = useState<ToastState | null>(null)

  const showToast = (message: string, type: ToastState['type'] = 'success') =>
    setToast({ message, type })

  useEffect(() => {
    getSettings()
      .then(({ data: d }) => setData(d))
      .catch(() => showToast('Failed to load settings.', 'error'))
      .finally(() => setLoading(false))
  }, [])

  // Debounced preference saver
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  function savePref(patch: Partial<SettingsData>) {
    if (!data) return
    const next = { ...data, ...patch }
    setData(next)
    if (saveTimer.current) clearTimeout(saveTimer.current)
    saveTimer.current = setTimeout(() => {
      updatePreferences(patch)
        .then(() => showToast('Preferences saved'))
        .catch(() => showToast('Failed to save', 'error'))
    }, 400)
  }

  if (loading || !data) {
    return (
      <div className="page-inner">
        <div className="page-header">
          <div><div className="eyebrow">Workspace</div><h1 className="title">Settings</h1></div>
        </div>
        <div className="sett-layout">
          <div className="sett-nav">
            {SECTIONS.map((s) => (
              <div key={s.id} className="an-skel" style={{ height: 36, marginBottom: 4, borderRadius: 8 }} />
            ))}
          </div>
          <div className="sett-content">
            <div className="an-skel" style={{ height: 24, width: 160, marginBottom: 32 }} />
            {[1,2,3].map((i) => (
              <div key={i} className="an-skel" style={{ height: 64, marginBottom: 12, borderRadius: 10 }} />
            ))}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="page-inner">
      <Toast toast={toast} onDismiss={() => setToast(null)} />

      <motion.div className="page-header"
        initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
      >
        <div>
          <div className="eyebrow">Workspace</div>
          <h1 className="title">Settings</h1>
        </div>
      </motion.div>

      <div className="sett-layout">
        {/* ── Sidebar ── */}
        <nav className="sett-nav">
          {SECTIONS.map((s) => (
            <button
              key={s.id}
              type="button"
              className={`sett-nav-item${section === s.id ? ' active' : ''}`}
              onClick={() => setSection(s.id)}
            >
              <s.icon />
              <span>{s.label}</span>
            </button>
          ))}
        </nav>

        {/* ── Content panel ── */}
        <div className="sett-content">
          <AnimatePresence mode="wait">
            <motion.div key={section}
              initial={{ opacity: 0, x: 8 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -8 }}
              transition={{ duration: 0.18 }}
            >
              {section === 'profile'       && <ProfileSection      data={data} onSaved={(name) => { setData({ ...data, name }); login(localStorage.getItem('token')!, data.email, name); showToast('Profile updated') }} onError={(m) => showToast(m, 'error')} />}
              {section === 'security'      && <SecuritySection     data={data} onSaved={() => showToast('Password updated')} onError={(m) => showToast(m, 'error')} />}
              {section === 'appearance'    && <AppearanceSection   theme={theme} density={density} onTheme={(t) => { setTheme(t); savePref({ darkMode: t === 'dark' }) }} onDensity={(d) => { setDensity(d); savePref({ density: d }) }} />}
              {section === 'storage'       && <StorageSection      data={data} onToggle={(k, v) => savePref({ [k]: v })} />}
              {section === 'notifications' && <NotificationsSection data={data} onToggle={(k, v) => savePref({ [k]: v })} />}
              {section === 'preferences'   && <PreferencesSection  data={data} onChange={(k, v) => savePref({ [k]: v })} />}
              {section === 'advanced'      && <AdvancedSection     data={data} onNewToken={(t) => setData({ ...data, apiToken: t })} onToggle={(k, v) => savePref({ [k]: v })} onToast={showToast} />}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// Section components
// ═══════════════════════════════════════════════════════════════════════════

function SectionHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="sett-section-head">
      <h2 className="sett-section-title">{title}</h2>
      {subtitle && <p className="sett-section-sub">{subtitle}</p>}
    </div>
  )
}

function SettingsCard({ children }: { children: React.ReactNode }) {
  return <div className="sett-card">{children}</div>
}

function SettingsRow({ label, description, children }: { label: string; description?: string; children: React.ReactNode }) {
  return (
    <div className="sett-row">
      <div className="sett-row-text">
        <span className="sett-row-label">{label}</span>
        {description && <span className="sett-row-desc">{description}</span>}
      </div>
      <div className="sett-row-ctrl">{children}</div>
    </div>
  )
}

// ── Profile ──────────────────────────────────────────────────────────────────
function ProfileSection({ data, onSaved, onError }: {
  data: SettingsData
  onSaved: (name: string) => void
  onError: (msg: string) => void
}) {
  const [name, setName]     = useState(data.name)
  const [saving, setSaving] = useState(false)

  async function handleSave() {
    if (!name.trim() || name.trim().length < 2) { onError('Name must be at least 2 characters'); return }
    setSaving(true)
    try {
      await updateProfile(name.trim())
      onSaved(name.trim())
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      onError(msg || 'Failed to update profile')
    } finally { setSaving(false) }
  }

  const initials = (data.name || '?').split(' ').map((p) => p[0]).join('').slice(0, 2).toUpperCase()

  return (
    <>
      <SectionHeader title="Profile" subtitle="Manage your public identity and account information." />

      <SettingsCard>
        {/* Avatar */}
        <div className="sett-avatar-row">
          <div className="sett-avatar">{initials}</div>
          <div>
            <div className="sett-avatar-name">{data.name}</div>
            <div className="sett-avatar-email">{data.email}</div>
          </div>
        </div>
        <div className="sett-divider" />

        <div className="sett-field-group">
          <label className="sett-label">Display name</label>
          <input className="sett-input" value={name} onChange={(e) => setName(e.target.value)}
            placeholder="Your name" maxLength={80} />
        </div>

        <div className="sett-field-group">
          <label className="sett-label">Email address</label>
          <input className="sett-input" value={data.email} disabled readOnly
            style={{ opacity: 0.5, cursor: 'not-allowed' }} />
          <span className="sett-hint">Email cannot be changed after registration.</span>
        </div>

        <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
          <button type="button" className="btn btn-accent" onClick={handleSave} disabled={saving || name.trim() === data.name}>
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </SettingsCard>

      <SettingsCard>
        <SettingsRow label="Member since"  description={data.memberSince ? new Date(data.memberSince).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'Unknown'}>
          <span />
        </SettingsRow>
        <div className="sett-divider" />
        <SettingsRow label="Last login" description={data.lastLogin ? new Date(data.lastLogin).toLocaleString() : 'First session'}>
          <span />
        </SettingsRow>
        <div className="sett-divider" />
        <SettingsRow label="Total files" description={`${data.totalFiles} file${data.totalFiles === 1 ? '' : 's'} stored`}>
          <span />
        </SettingsRow>
      </SettingsCard>
    </>
  )
}

// ── Security ─────────────────────────────────────────────────────────────────
function SecuritySection({ data, onSaved, onError }: {
  data: SettingsData
  onSaved: () => void
  onError: (msg: string) => void
}) {
  const [form, setForm]     = useState({ current: '', next: '', confirm: '' })
  const [saving, setSaving] = useState(false)
  const [show, setShow]     = useState(false)

  async function handleSave() {
    if (form.next.length < 8)        { onError('Password must be at least 8 characters'); return }
    if (form.next !== form.confirm)  { onError('Passwords do not match'); return }
    setSaving(true)
    try {
      await updatePassword(form.current, form.next)
      setForm({ current: '', next: '', confirm: '' })
      onSaved()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      onError(msg || 'Failed to update password')
    } finally { setSaving(false) }
  }

  return (
    <>
      <SectionHeader title="Security" subtitle="Update your password to keep your account secure." />

      {!data.hasPassword && (
        <div className="sett-info-banner">
          <InfoIcon /> You signed in with Google. Set a password below to also enable email/password login.
        </div>
      )}

      <SettingsCard>
        {data.hasPassword && (
          <div className="sett-field-group">
            <label className="sett-label">Current password</label>
            <input className="sett-input" type={show ? 'text' : 'password'} value={form.current}
              onChange={(e) => setForm({ ...form, current: e.target.value })}
              placeholder="••••••••" />
          </div>
        )}
        <div className="sett-field-group">
          <label className="sett-label">New password</label>
          <input className="sett-input" type={show ? 'text' : 'password'} value={form.next}
            onChange={(e) => setForm({ ...form, next: e.target.value })}
            placeholder="At least 8 characters" />
        </div>
        <div className="sett-field-group">
          <label className="sett-label">Confirm new password</label>
          <input className="sett-input" type={show ? 'text' : 'password'} value={form.confirm}
            onChange={(e) => setForm({ ...form, confirm: e.target.value })}
            placeholder="Repeat your new password" />
        </div>

        {/* Password strength bar */}
        {form.next && <PasswordStrength password={form.next} />}

        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 8 }}>
          <button type="button" className="btn btn-accent" onClick={handleSave}
            disabled={saving || !form.next || form.next !== form.confirm}>
            {saving ? 'Updating…' : 'Update password'}
          </button>
          <label className="sett-show-pw">
            <input type="checkbox" checked={show} onChange={(e) => setShow(e.target.checked)} />
            Show passwords
          </label>
        </div>
      </SettingsCard>
    </>
  )
}

function PasswordStrength({ password }: { password: string }) {
  const score = [password.length >= 8, /[A-Z]/.test(password), /[0-9]/.test(password), /[^A-Za-z0-9]/.test(password)].filter(Boolean).length
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong']
  const colors = ['', 'var(--danger)', 'var(--warn)', 'var(--info)', 'var(--ok)']
  return (
    <div className="sett-pw-strength">
      <div className="sett-pw-bars">
        {[1,2,3,4].map((i) => (
          <div key={i} className="sett-pw-bar" style={{ background: i <= score ? colors[score] : 'var(--surface-3)' }} />
        ))}
      </div>
      <span style={{ fontSize: 11, color: colors[score], fontFamily: 'var(--mono)', fontWeight: 600 }}>{labels[score]}</span>
    </div>
  )
}

// ── Appearance ───────────────────────────────────────────────────────────────
function AppearanceSection({ theme, density, onTheme, onDensity }: {
  theme: string
  density: string
  onTheme: (t: 'light' | 'dark') => void
  onDensity: (d: 'compact' | 'comfortable' | 'airy') => void
}) {
  return (
    <>
      <SectionHeader title="Appearance" subtitle="Personalise how Vault looks for you." />
      <SettingsCard>
        <SettingsRow label="Dark mode" description="Switch between light and dark interface.">
          <ToggleSwitch checked={theme === 'dark'} onChange={(v) => onTheme(v ? 'dark' : 'light')} />
        </SettingsRow>
      </SettingsCard>

      <SettingsCard>
        <div className="sett-row-label" style={{ marginBottom: 14 }}>Display density</div>
        <div className="sett-density-grid">
          {(['compact', 'comfortable', 'airy'] as const).map((d) => (
            <button key={d} type="button"
              className={`sett-density-opt${density === d ? ' active' : ''}`}
              onClick={() => onDensity(d)}
            >
              <div className="sett-density-preview">
                {d === 'compact'     && <><div className="dp-line s" /><div className="dp-line m" /><div className="dp-line s" /></>}
                {d === 'comfortable' && <><div className="dp-line m" /><div className="dp-gap" /><div className="dp-line l" /><div className="dp-gap" /><div className="dp-line m" /></>}
                {d === 'airy'        && <><div className="dp-line l" /><div className="dp-gap lg" /><div className="dp-line m" /><div className="dp-gap lg" /><div className="dp-line l" /></>}
              </div>
              <span>{d.charAt(0).toUpperCase() + d.slice(1)}</span>
            </button>
          ))}
        </div>
      </SettingsCard>
    </>
  )
}

// ── Storage ──────────────────────────────────────────────────────────────────
function StorageSection({ data, onToggle }: {
  data: SettingsData
  onToggle: (key: string, value: boolean) => void
}) {
  const pct = Math.min(data.storagePercentage, 100)
  const barColor = pct > 80 ? 'var(--danger)' : pct > 60 ? 'var(--warn)' : '#CFFF3D'

  return (
    <>
      <SectionHeader title="Storage" subtitle="Monitor your usage and configure storage behaviour." />

      {/* Mini overview */}
      <SettingsCard>
        <div className="sett-storage-overview">
          <div className="sett-storage-nums">
            <span className="sett-storage-used">{formatBytes(data.storageUsed)}</span>
            <span className="sett-storage-of">of {formatBytes(data.storageTotal)} used</span>
          </div>
          <div className="sett-storage-bar-track">
            <div className="sett-storage-bar-fill"
              style={{ width: `${pct}%`, background: barColor, transition: 'width 0.8s ease' }} />
          </div>
          <div className="sett-storage-meta">
            <span>{pct.toFixed(1)}% utilized · {data.totalFiles} files</span>
            <span>{formatBytes(data.storageTotal - data.storageUsed)} free</span>
          </div>
        </div>
      </SettingsCard>

      <SettingsCard>
        <ToggleSwitch
          checked={data.storageWarningEnabled}
          onChange={(v) => onToggle('storageWarningEnabled', v)}
          label="Storage warning"
          description="Get notified when storage exceeds 80%."
        />
        <div className="sett-divider" />
        <ToggleSwitch
          checked={data.autoOrganize}
          onChange={(v) => onToggle('autoOrganize', v)}
          label="Auto-organize files"
          description="Automatically group uploaded files into folders by type."
        />
      </SettingsCard>
    </>
  )
}

// ── Notifications ────────────────────────────────────────────────────────────
function NotificationsSection({ data, onToggle }: {
  data: SettingsData
  onToggle: (key: string, value: boolean) => void
}) {
  return (
    <>
      <SectionHeader title="Notifications" subtitle="Choose what you want to be notified about." />

      <SettingsCard>
        <div className="sett-card-sub-label">Email notifications</div>
        <ToggleSwitch
          checked={data.emailNotificationsEnabled}
          onChange={(v) => onToggle('emailNotificationsEnabled', v)}
          label="Email notifications"
          description="Master switch for all email alerts."
        />
        <div className="sett-divider" />
        <ToggleSwitch
          checked={data.storageWarningEnabled}
          onChange={(v) => onToggle('storageWarningEnabled', v)}
          label="Storage almost full"
          description="Alert when usage exceeds 80%."
          disabled={!data.emailNotificationsEnabled}
        />
        <div className="sett-divider" />
        <ToggleSwitch
          checked={data.uploadNotifications}
          onChange={(v) => onToggle('uploadNotifications', v)}
          label="File upload"
          description="Confirm each successful upload by email."
          disabled={!data.emailNotificationsEnabled}
        />
        <div className="sett-divider" />
        <ToggleSwitch
          checked={data.deleteNotifications}
          onChange={(v) => onToggle('deleteNotifications', v)}
          label="File deletion"
          description="Email when a file is moved to trash."
          disabled={!data.emailNotificationsEnabled}
        />
      </SettingsCard>

      <SettingsCard>
        <div className="sett-card-sub-label">In-app notifications</div>
        <ToggleSwitch
          checked={data.inAppNotifications}
          onChange={(v) => onToggle('inAppNotifications', v)}
          label="In-app notifications"
          description="Show banners and toasts inside the app."
        />
      </SettingsCard>
    </>
  )
}

// ── Preferences ──────────────────────────────────────────────────────────────
function PreferencesSection({ data, onChange }: {
  data: SettingsData
  onChange: (key: string, value: string) => void
}) {
  return (
    <>
      <SectionHeader title="Preferences" subtitle="Set defaults for how files are displayed and sorted." />

      <SettingsCard>
        <SettingsRow label="Default view" description="How files are shown when you open a folder.">
          <div className="sett-seg">
            {(['grid', 'list'] as const).map((v) => (
              <button key={v} type="button"
                className={`sett-seg-opt${data.defaultView === v ? ' active' : ''}`}
                onClick={() => onChange('defaultView', v)}
              >
                {v === 'grid' ? <GridIcon /> : <ListIcon />}
                <span>{v.charAt(0).toUpperCase() + v.slice(1)}</span>
              </button>
            ))}
          </div>
        </SettingsRow>
        <div className="sett-divider" />
        <SettingsRow label="Default sort" description="Order used when listing files.">
          <div className="sett-seg">
            {(['name', 'size', 'date'] as const).map((v) => (
              <button key={v} type="button"
                className={`sett-seg-opt${data.defaultSort === v ? ' active' : ''}`}
                onClick={() => onChange('defaultSort', v)}
              >
                <span>{v.charAt(0).toUpperCase() + v.slice(1)}</span>
              </button>
            ))}
          </div>
        </SettingsRow>
      </SettingsCard>
    </>
  )
}

// ── Advanced ─────────────────────────────────────────────────────────────────
function AdvancedSection({ data, onNewToken, onToggle, onToast }: {
  data: SettingsData
  onNewToken: (t: string) => void
  onToggle: (key: string, value: boolean) => void
  onToast: (msg: string, type?: ToastState['type']) => void
}) {
  const [regenerating, setRegenerating] = useState(false)
  const [copied, setCopied]             = useState(false)

  async function handleRegenerate() {
    if (!confirm('Regenerate your API token? The current token will stop working immediately.')) return
    setRegenerating(true)
    try {
      const { data: { token } } = await regenerateApiToken()
      onNewToken(token)
      onToast('New API token generated')
    } catch { onToast('Failed to regenerate token', 'error') }
    finally { setRegenerating(false) }
  }

  function handleCopy() {
    if (!data.apiToken) return
    navigator.clipboard.writeText(data.apiToken).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }

  return (
    <>
      <SectionHeader title="Advanced" subtitle="Developer tools and experimental features." />

      <SettingsCard>
        <div className="sett-card-sub-label">API Access</div>
        <div className="sett-api-token-block">
          <div className="sett-api-token-field">
            <code className="sett-api-token-val">{data.apiToken ?? '—'}</code>
            <button type="button" className="sett-api-copy" onClick={handleCopy} title="Copy token">
              {copied ? <CheckIcon /> : <CopyIcon />}
            </button>
          </div>
          <div className="sett-api-hint">Use this token to authenticate API requests. Keep it secret.</div>
          <button type="button" className="btn" onClick={handleRegenerate} disabled={regenerating}
            style={{ marginTop: 10 }}>
            {regenerating ? 'Generating…' : 'Regenerate token'}
          </button>
        </div>
      </SettingsCard>

      <SettingsCard>
        <div className="sett-card-sub-label">Developer</div>
        <ToggleSwitch
          checked={data.debugMode}
          onChange={(v) => onToggle('debugMode', v)}
          label="Debug mode"
          description="Log verbose output to the browser console."
        />
      </SettingsCard>

      <div className="sett-danger-zone">
        <div className="sett-danger-label">Danger zone</div>
        <p className="sett-danger-desc">Destructive actions — these cannot be undone.</p>
        <button type="button" className="btn"
          style={{ background: 'color-mix(in oklab, var(--danger) 12%, var(--surface))', color: 'var(--danger)', border: '1px solid color-mix(in oklab, var(--danger) 30%, transparent)' }}
          onClick={() => onToast('Account deletion is disabled in this demo.', 'error')}
        >
          Delete account
        </button>
      </div>
    </>
  )
}

