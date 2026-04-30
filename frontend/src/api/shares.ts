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

export interface CreateSharePayload {
  sharedWithEmail?: string | null
  permission?: 'VIEW' | 'DOWNLOAD'
  expiresAt?: string | null
}

export const createShare = (fileId: number, payload: CreateSharePayload) =>
  client.post<ShareItem>(`/shares/files/${fileId}`, payload)

export const getSharesForFile = (fileId: number) =>
  client.get<ShareItem[]>(`/shares/files/${fileId}`)

export const revokeShare = (shareId: number) =>
  client.delete(`/shares/${shareId}`)

export const getSharedWithMe = () =>
  client.get<ShareItem[]>('/shares/shared-with-me')

export const resolvePublicLink = (token: string) =>
  client.get(`/shares/public/${token}`)