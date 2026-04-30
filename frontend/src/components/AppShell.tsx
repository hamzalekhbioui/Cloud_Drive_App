import { useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import Icon from '../components/Icon'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

const NAV = [
  { to: '/', label: 'Dashboard', icon: 'home' as const, end: true },
  { to: '/files', label: 'All files', icon: 'folder' as const },
  { to: '/shared', label: 'Shared', icon: 'users' as const },
  { to: '/starred', label: 'Starred', icon: 'star' as const },
]

const NAV_WORKSPACE = [
  { to: '/analytics', label: 'Storage analytics', icon: 'chart' as const },
  { to: '/trash', label: 'Trash', icon: 'trash' as const },
  { to: '/settings', label: 'Settings', icon: 'settings' as const },
]

export default function AppShell() {
  const { user, logout } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileNav, setMobileNav] = useState(false)
  const [searchValue, setSearchValue] = useState(() => searchParams.get('q') ?? '')

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    const q = e.target.value
    setSearchValue(q)
    if (q.trim()) {
      navigate(`/files?q=${encodeURIComponent(q.trim())}`)
    } else if (location.pathname === '/files') {
      navigate('/files')
    }
  }

  const breadcrumbs = (() => {
    const path = location.pathname
    if (path === '/') return ['Home']
    const segment = path.slice(1).split('/')[0]
    const map: Record<string, string> = {
      files: 'All files', shared: 'Shared', starred: 'Starred',
      analytics: 'Storage analytics', trash: 'Trash', settings: 'Settings',
    }
    return ['Home', map[segment] || segment]
  })()

  return (
    <div className="app" data-sidebar={collapsed ? 'collapsed' : 'open'} data-mobile-nav={mobileNav ? 'open' : 'closed'}>
      <aside className={`sidebar ${collapsed ? 'collapsed' : ''}`}>
        <button className="collapse-toggle" onClick={() => setCollapsed((v) => !v)} aria-label="Toggle sidebar">
          <Icon name={collapsed ? 'chevronRight' : 'chevronLeft'} size={12} />
        </button>

        <div className="sidebar-brand">
          <img src="/cloud_drive_app.png" alt="Vault" className="logo" style={{ objectFit: 'contain' }} />
          <div className="brand-name">Vault</div>
        </div>

        <div className="nav-section">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              onClick={() => setMobileNav(false)}
            >
              <Icon name={item.icon} size={16} />
              <span className="nav-label">{item.label}</span>
            </NavLink>
          ))}
        </div>

        <div className="nav-section">
          <div className="nav-section-label">Workspace</div>
          {NAV_WORKSPACE.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              onClick={() => setMobileNav(false)}
            >
              <Icon name={item.icon} size={16} />
              <span className="nav-label">{item.label}</span>
            </NavLink>
          ))}
        </div>

        <div className="sidebar-footer">
          {!collapsed && (
            <div className="storage-card">
 ru              {user ? (
                <>
                  <div className="eyebrow">Signed in as</div>
                  <div style={{ fontSize: 13, fontWeight: 600, marginTop: 4 }}>{user.name}</div>
                  <div style={{ fontSize: 11, color: 'var(--ink-3)', fontFamily: 'var(--mono)' }}>{user.email}</div>
                  <button className="upgrade" onClick={handleLogout} style={{ background: 'var(--surface-3)', color: 'var(--ink)' }}>Sign out</button>
                </>
              ) : (
                <button className="upgrade" onClick={() => navigate('/login')} style={{ background: 'var(--surface-3)', color: 'var(--ink)' }}>Sign in</button>
              )}
            </div>
          )}
        </div>
      </aside>
      <div className="mobile-scrim" onClick={() => setMobileNav(false)} />

      <main className="main">
        <header className="topbar">
          <button className="icon-btn mobile-menu" onClick={() => setMobileNav(true)} aria-label="Menu">
            <Icon name="menu" size={18} />
          </button>
          <nav className="breadcrumbs" aria-label="Breadcrumb">
            {breadcrumbs.map((b, i) => (
              <span key={i} style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                {i > 0 && <span className="sep"><Icon name="chevronRight" size={12} /></span>}
                <span className={i === breadcrumbs.length - 1 ? 'current' : ''}>{b}</span>
              </span>
            ))}
          </nav>
          <div className="search">
            <span className="search-icon"><Icon name="search" size={15} /></span>
            <input
              placeholder="Search files…"
              value={searchValue}
              onChange={handleSearch}
            />
            <span className="kbd">⌘K</span>
          </div>
          <div className="topbar-actions">
            <button className="icon-btn" onClick={toggleTheme} aria-label="Toggle theme">
              <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={17} />
            </button>
            <button className="icon-btn" aria-label="Notifications">
              <Icon name="bell" size={17} />
            </button>
            <button className="avatar-btn" aria-label="Profile" onClick={() => { if (!user) navigate('/login') }}>
              {user ? user.name.charAt(0).toUpperCase() : '?'}
            </button>
          </div>
          <style>{`.mobile-menu{display:none} @media (max-width: 820px){ .mobile-menu{ display: grid !important; } }`}</style>
        </header>
        <div className="page">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
