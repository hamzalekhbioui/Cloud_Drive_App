import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import Icon from '../../components/Icon'
import { useTheme } from '../../context/ThemeContext'
import { useAdminAuth } from '../context/AdminAuthContext'

const NAV = [
  { to: '/admin', label: 'Overview', icon: 'home' as const, end: true },
  { to: '/admin/users', label: 'Users', icon: 'users' as const },
  { to: '/admin/files', label: 'Files', icon: 'folder' as const },
  { to: '/admin/shares', label: 'Share links', icon: 'share' as const },
  { to: '/admin/teams', label: 'Teams', icon: 'users' as const },
  { to: '/admin/billing', label: 'Billing', icon: 'tag' as const },
  { to: '/admin/webhooks', label: 'Webhooks', icon: 'shield' as const },
  { to: '/admin/audit', label: 'Audit log', icon: 'clock' as const },
  { to: '/admin/settings', label: 'Settings', icon: 'settings' as const },
]

export default function AdminShell() {
  const { user, logout } = useAdminAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()

  const breadcrumbs = location.pathname === '/admin'
    ? ['Admin', 'Overview']
    : ['Admin', location.pathname.slice('/admin/'.length)]

  function handleLogout() {
    logout()
    navigate('/admin/login')
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <img src="/cloude_logo.jpeg" alt="Vault" className="logo" style={{ objectFit: 'contain' }} />
          <div className="brand-name">Vault Admin</div>
        </div>
        <div className="nav-section">
          <div className="nav-section-label">Administration</div>
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              <Icon name={item.icon} size={16} />
              <span className="nav-label">{item.label}</span>
            </NavLink>
          ))}
        </div>
        <div className="sidebar-footer">
          <div className="storage-card">
            <div className="eyebrow">Signed in as</div>
            <div style={{ fontSize: 13, fontWeight: 600, marginTop: 4 }}>{user?.name}</div>
            <div style={{ fontSize: 11, color: 'var(--ink-3)', fontFamily: 'var(--mono)' }}>{user?.email}</div>
            <button className="upgrade" onClick={handleLogout} style={{ background: 'var(--surface-3)', color: 'var(--ink)' }}>Sign out</button>
          </div>
        </div>
      </aside>
      <main className="main">
        <header className="topbar">
          <nav className="breadcrumbs" aria-label="Breadcrumb">
            {breadcrumbs.map((breadcrumb, index) => (
              <span key={breadcrumb} style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                {index > 0 && <span className="sep"><Icon name="chevronRight" size={12} /></span>}
                <span className={index === breadcrumbs.length - 1 ? 'current' : ''}>{breadcrumb}</span>
              </span>
            ))}
          </nav>
          <div className="topbar-actions">
            <button className="icon-btn" onClick={toggleTheme} aria-label="Toggle theme">
              <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={17} />
            </button>
            <button className="avatar-btn" aria-label="Admin profile">{user?.name.charAt(0).toUpperCase()}</button>
          </div>
        </header>
        <div className="page">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
