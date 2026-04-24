import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'

interface AuthUser {
  email: string
  name: string
}

interface AuthContextType {
  user: AuthUser | null
  login: (token: string, email: string, name: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType>(null!)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const token = localStorage.getItem('token')
    const email = localStorage.getItem('email')
    const name = localStorage.getItem('name')
    return token && email && name ? { email, name } : null
  })

  function login(token: string, email: string, name: string) {
    localStorage.setItem('token', token)
    localStorage.setItem('email', email)
    localStorage.setItem('name', name)
    setUser({ email, name })
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('email')
    localStorage.removeItem('name')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)