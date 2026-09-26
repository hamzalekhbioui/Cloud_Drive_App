import client from './client'
import axios from 'axios'

export interface FileItem {
  id: number
  originalFileName: string
  url: string
  size: number
  type: string
  createdAt: string
  starred: boolean
  deletedAt: string | null
  status: 'ACTIVE' | 'PENDING'
  userId: string
  folderId?: number | null
  aiStatus?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'ERROR' | 'UNSUPPORTED'
  aiError?: string | null
  aiSummary?: string | null
}

export interface AiStatus {
  status: FileItem['aiStatus']
  error: string | null
  summary: string | null
  processedAt: string | null
}

export interface AiCitation {
  chunkIndex: number
  source: string | null
  excerpt: string
  score: number
}

export interface ChatResponse {
  answer: string
  citations: AiCitation[]
}

export interface UploadTarget {
  uploadId: number
  writeUrl: string
  blobKey: string
  ttlSec: number
}

export interface PageResponse<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface FilePageOptions {
  page?: number
  size?: number
  q?: string
  sort?: string
}

const pageParams = (options: FilePageOptions = {}) => ({ size: 50, ...options })

export const getMyFiles = (options?: FilePageOptions) =>
  client.get<PageResponse<FileItem>>('/files/me', { params: pageParams(options) })
export const getStarredFiles = (options?: FilePageOptions) =>
  client.get<PageResponse<FileItem>>('/files/starred', { params: pageParams(options) })
export const getTrashFiles = (options?: FilePageOptions) =>
  client.get<PageResponse<FileItem>>('/files/trash', { params: pageParams(options) })
export const getTeamFiles = (teamId: number, options?: FilePageOptions) =>
  client.get<PageResponse<FileItem>>(`/files/team/${teamId}`, { params: pageParams(options) })
export const renameFile = (fileId: number, name: string) =>
  client.patch<FileItem>(`/files/${fileId}/name`, { name })
export const moveFile = (fileId: number, folderId: number | null) =>
  client.patch<FileItem>(`/files/${fileId}/folder`, null, { params: { folderId } })

export const deleteFile = (fileId: number) => client.delete(`/files/${fileId}`)
export const restoreFile = (fileId: number) => client.post(`/files/${fileId}/restore`)
export const permanentlyDeleteFile = (fileId: number) => client.delete(`/files/${fileId}/permanent`)
export const starFile = (fileId: number) => client.patch<FileItem>(`/files/${fileId}/star`)
export const getAiStatus = (fileId: number) => client.get<AiStatus>(`/files/${fileId}/ai-status`)
export const retryAi = (fileId: number) => client.post<AiStatus>(`/files/${fileId}/ai-status/retry`)
export const chatWithFile = (fileId: number, message: string) =>
  client.post<ChatResponse>(`/files/${fileId}/chat`, { message })

// ── two-phase direct-to-Azure upload ──────────────────────────────────────

/** Phase 1: ask the backend to mint a short-lived write SAS URL. */
const requestUploadTarget = (file: File, teamId?: number) =>
  client.post<UploadTarget>('/files/upload/begin', {
    size: file.size,
    rawFileName: file.name,
    teamId
  })

/** Legacy upload via backend proxy (multipart). */
export const uploadMultipart = async (
  file: File,
  onProgress?: (pct: number) => void,
  teamId?: number
): Promise<FileItem> => {
  const formData = new FormData()
  formData.append('file', file)
  if (teamId) formData.append('teamId', teamId.toString())
  const { data } = await client.post<FileItem>('/files/upload', formData, {
    onUploadProgress: (e) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
    },
  })
  return data
}

/**
 * Upload a file directly to Azure via a pre-signed SAS URL.
 * Fallback to multipart if direct upload fails (e.g. CORS/Network issues).
 */
export const uploadFile = async (
  file: File,
  onProgress?: (pct: number) => void,
  teamId?: number
): Promise<FileItem> => {
  try {
    const { data: target } = await requestUploadTarget(file, teamId)

    await axios.put(target.writeUrl, file, {
      headers: {
        'x-ms-blob-type': 'BlockBlob',
        'Content-Type': file.type,
        'x-ms-blob-content-disposition': 'inline',
      },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
      },
    })

    const { data } = await client.post<FileItem>(`/files/upload/${target.uploadId}/commit`)
    return data
  } catch (err: unknown) {
    const error = err as { response?: unknown; request?: unknown; config?: { url?: string }; message?: string }
    // If it's a CORS error (no response) or Azure-specific error, try the legacy multipart upload
    const isNetworkOrCorsError = !error.response && error.request
    const isAzureError = error.config?.url?.includes('blob.core.windows.net')

    if (isNetworkOrCorsError || isAzureError) {
      console.warn('Direct upload failed (possibly CORS). Falling back to multipart proxy upload.', err)
      return uploadMultipart(file, onProgress, teamId)
    }
    throw err
  }
}
