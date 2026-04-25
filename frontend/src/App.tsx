import { Suspense, lazy, useEffect, useMemo, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { Helmet } from 'react-helmet-async'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import PrivateRoute from './components/PrivateRoute'
import AppShell from './components/AppShell'
import { pingBackend } from './api/health'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const RegisterPage = lazy(() => import('./pages/RegisterPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const FilesPage = lazy(() => import('./pages/FilesPage'))
const StarredPage = lazy(() => import('./pages/StarredPage'))
const TrashPage = lazy(() => import('./pages/TrashPage'))
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage'))
const SettingsPage = lazy(() => import('./pages/SettingsPage'))

function RouteMeta() {
  const { pathname } = useLocation()

  const title = useMemo(() => {
    if (pathname === '/login') return 'Sign In | Vault'
    if (pathname === '/register') return 'Create Account | Vault'
    if (pathname === '/') return 'Dashboard | Vault'
    if (pathname.startsWith('/files')) return 'All Files | Vault'
    if (pathname.startsWith('/starred')) return 'Starred Files | Vault'
    if (pathname.startsWith('/trash')) return 'Trash | Vault'
    if (pathname.startsWith('/analytics')) return 'Analytics | Vault'
    if (pathname.startsWith('/settings')) return 'Settings | Vault'
    return 'Vault'
  }, [pathname])

  return (
    <Helmet>
      <title>{title}</title>
      <meta property="og:title" content={title} />
    </Helmet>
  )
}

function RouteFallback() {
  return <div style={{ textAlign: 'center', padding: 40, color: 'var(--ink-3)' }}>Loading page…</div>
}

export default function App() {
  const [backendOffline, setBackendOffline] = useState(false)
  const [healthChecking, setHealthChecking] = useState(true)

  async function checkBackend() {
    setHealthChecking(true)
    try {
      await pingBackend()
      setBackendOffline(false)
    } catch {
      setBackendOffline(true)
    } finally {
      setHealthChecking(false)
    }
  }

  useEffect(() => {
    void checkBackend()
  }, [])

  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <RouteMeta />

          {backendOffline && (
            <div style={{ margin: 12, padding: 12, borderRadius: 10, background: 'color-mix(in oklab, var(--warn) 14%, var(--surface))', color: 'var(--warn)', fontSize: 13 }}>
              Backend appears offline. Start Spring Boot and retry to avoid API/CORS errors.
              <button type="button" className="btn" style={{ marginLeft: 10, height: 30 }} onClick={() => void checkBackend()} disabled={healthChecking}>
                {healthChecking ? 'Checking…' : 'Retry'}
              </button>
            </div>
          )}

          <Suspense fallback={<RouteFallback />}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route
                element={
                  <PrivateRoute>
                    <AppShell />
                  </PrivateRoute>
                }
              >
                <Route path="/" element={<DashboardPage />} />
                <Route path="/files" element={<FilesPage />} />
                <Route path="/shared" element={<FilesPage />} />
                <Route path="/starred" element={<StarredPage />} />
                <Route path="/trash" element={<TrashPage />} />
                <Route path="/analytics" element={<AnalyticsPage />} />
                <Route path="/settings" element={<SettingsPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}
