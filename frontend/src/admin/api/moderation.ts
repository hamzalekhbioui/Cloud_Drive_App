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

export const getAdminFiles = (page = 0) => adminClient.get<Page<AdminFile>>('/files', { params: { page } })
export const deleteAdminFile = (id: number) => adminClient.post<AdminFile>(`/files/${id}/delete`)
export const restoreAdminFile = (id: number) => adminClient.post<AdminFile>(`/files/${id}/restore`)
export const getAdminShares = (page = 0) => adminClient.get<Page<AdminShare>>('/shares', { params: { page } })
export const revokeAdminShare = (id: number) => adminClient.post<AdminShare>(`/shares/${id}/revoke`)
export const getAdminTeams = (page = 0) => adminClient.get<Page<AdminTeam>>('/teams', { params: { page } })
