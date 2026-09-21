import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAdminAuth } from '../context/AdminAuthContext'

export default function AdminRoute({ children }: { children: ReactNode }) {
  const { user } = useAdminAuth()
  return user ? <>{children}</> : <Navigate to="/admin/login" replace />
}
