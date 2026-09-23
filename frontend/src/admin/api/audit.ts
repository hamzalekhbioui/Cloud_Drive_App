import adminClient from './adminClient'

export interface AdminAuditLog {
  id: number
  adminId: number | null
  adminEmail: string
  action: string
  targetType: string | null
  targetId: string | null
  detailJson: string | null
  ip: string | null
  createdAt: string
}

export interface AuditPage {
  content: AdminAuditLog[]
  totalPages: number
  number: number
}

export interface AuditFilters {
  admin?: string
  action?: string
  targetType?: string
  fromDate?: string
  toDate?: string
}

export const getAdminAudit = (page = 0, filters: AuditFilters = {}) =>
  adminClient.get<AuditPage>('/audit', { params: { page, ...filters } })
