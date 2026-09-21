/* eslint react-refresh/only-export-components: off */
import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'

interface AdminUser {
  email: string
  name: string
}

interface AdminAuthContextType {
  user: AdminUser | null
  login: (token: string, email: string, name: string) => void
  logout: () => void
}

const AdminAuthContext = createContext<AdminAuthContextType>(null!)

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AdminUser | null>(() => {
    const token = localStorage.getItem('admin_token')
    const email = localStorage.getItem('admin_email')
    const name = localStorage.getItem('admin_name')
    return token && email && name ? { email, name } : null
  })

  function login(token: string, email: string, name: string) {
    localStorage.setItem('admin_token', token)
    localStorage.setItem('admin_email', email)
    localStorage.setItem('admin_name', name)
    setUser({ email, name })
  }

  function logout() {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_email')
    localStorage.removeItem('admin_name')
    setUser(null)
  }

  return <AdminAuthContext.Provider value={{ user, login, logout }}>{children}</AdminAuthContext.Provider>
}

export const useAdminAuth = () => useContext(AdminAuthContext)
