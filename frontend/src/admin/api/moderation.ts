import adminClient from './adminClient'

export interface AdminFile {
  id: number
  fileName: string
  ownerEmail: string
  teamId: number | null
  status: string
  type: string
  size: number
  createdAt: string | null
  deletedAt: string | null
  aiStatus?: string
  teamName?: string
  shares?: AdminShare[]
}

export interface AdminShare {
  id: number
  fileId: number
  fileName?: string
  ownerEmail: string
  sharedWithEmail?: string
  permission: string
  createdAt: string | null
  expiresAt: string | null
  revokedAt: string | null
}

export interface AdminTeam {
  id: number
  name: string
  ownerEmail: string
  createdAt: string | null
  members: { id: number; userEmail: string; role: string; status: string }[]
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

export interface AdminFileFilters {
  owner?: string
  status?: string
  type?: string
  minSize?: number
  maxSize?: number
  fromDate?: string
  toDate?: string
}

export const getAdminFiles = (page = 0, filters: AdminFileFilters = {}) =>
  adminClient.get<Page<AdminFile>>('/files', { params: { page, ...filters } })
export const deleteAdminFile = (id: number) => adminClient.post<AdminFile>(`/files/${id}/delete`)
export const restoreAdminFile = (id: number) => adminClient.post<AdminFile>(`/files/${id}/restore`)
export const purgeAdminFile = (id: number) => adminClient.delete(`/files/${id}`)
export const getAdminShares = (page = 0) => adminClient.get<Page<AdminShare>>('/shares', { params: { page } })
export const revokeAdminShare = (id: number) => adminClient.post<AdminShare>(`/shares/${id}/revoke`)
export const getAdminTeams = (page = 0) => adminClient.get<Page<AdminTeam>>('/teams', { params: { page } })
