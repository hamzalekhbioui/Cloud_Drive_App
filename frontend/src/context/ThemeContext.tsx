import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'

type Theme = 'light' | 'dark'
type Density = 'compact' | 'comfortable' | 'airy'

interface ThemeCtx {
  theme: Theme
  setTheme: (t: Theme) => void
  toggleTheme: () => void
  density: Density
  setDensity: (d: Density) => void
}

const Ctx = createContext<ThemeCtx>(null!)

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => (localStorage.getItem('theme') as Theme) || 'light')
  const [density, setDensity] = useState<Density>(() => (localStorage.getItem('density') as Density) || 'comfortable')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('theme', theme)
  }, [theme])

  useEffect(() => {
    document.documentElement.setAttribute('data-density', density)
    localStorage.setItem('density', density)
  }, [density])

  const toggleTheme = () => setTheme((t) => (t === 'light' ? 'dark' : 'light'))

  return (
    <Ctx.Provider value={{ theme, setTheme, toggleTheme, density, setDensity }}>
      {children}
    </Ctx.Provider>
  )
}

export const useTheme = () => useContext(Ctx)