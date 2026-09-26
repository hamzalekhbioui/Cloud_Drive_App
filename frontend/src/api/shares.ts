import client from './client'

export interface ShareItem {
  id: number
  fileId: number
  fileName: string
  ownerEmail: string
  sharedWithEmail: string | null
  token: string
  permission: 'VIEW' | 'DOWNLOAD'
  createdAt: string
  expiresAt: string | null
  publicLink?: string
}

export interface SharedWithMeItem {
  id: number
  fileId: number
  fileName: string
  ownerEmail: string
  permission: 'VIEW' | 'DOWNLOAD'
  size?: number
  type?: string
  createdAt: string
  expiresAt: string | null
}

export interface CreateSharePayload {
  sharedWithEmail?: string | null
  permission?: 'VIEW' | 'DOWNLOAD'
  expiresAt?: string | null
}

export interface PageResponse<T> {
  content: T[]
  number: number
  totalElements: number
  totalPages: number
  last: boolean
}

export const createShare = (fileId: number, payload: CreateSharePayload) =>
  client.post<ShareItem>(`/documents/${fileId}/shares`, payload)

export const getSharesForFile = (fileId: number) =>
  client.get<ShareItem[]>(`/documents/${fileId}/shares`)

export const revokeShareForFile = (fileId: number) =>
  client.delete(`/documents/${fileId}/shares`)

export const revokeShare = (shareId: number) =>
  client.delete(`/shares/${shareId}`)

export const getSharedWithMe = (page = 0, size = 50) =>
  client.get<PageResponse<SharedWithMeItem>>('/shares/shared-with-me', { params: { page, size } })

export const fetchSharedFile = (shareId: number, download = false) =>
  client.get<Blob>(`/shares/shared-with-me/${shareId}/stream`, {
    params: { download },
    responseType: 'blob',
  })

export const resolvePublicLink = (token: string) =>
  client.get(`/shares/public/${token}`)
