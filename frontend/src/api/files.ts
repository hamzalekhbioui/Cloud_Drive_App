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
}

export interface UploadTarget {
  uploadId: number
  writeUrl: string
  blobKey: string
  ttlSec: number
}

export const getMyFiles = () => client.get<FileItem[]>('/files/me')
export const getStarredFiles = () => client.get<FileItem[]>('/files/starred')
export const getTrashFiles = () => client.get<FileItem[]>('/files/trash')

export const deleteFile = (fileId: number) => client.delete(`/files/${fileId}`)
export const restoreFile = (fileId: number) => client.post(`/files/${fileId}/restore`)
export const permanentlyDeleteFile = (fileId: number) => client.delete(`/files/${fileId}/permanent`)
export const starFile = (fileId: number) => client.patch<FileItem>(`/files/${fileId}/star`)

// ── two-phase direct-to-Azure upload ──────────────────────────────────────

/** Phase 1: ask the backend to mint a short-lived write SAS URL. */
const requestUploadTarget = (file: File) =>
  client.post<UploadTarget>('/files/upload/begin', {
    size: file.size,
    rawFileName: file.name,
  })

/**
 * Upload a file directly to Azure via a pre-signed SAS URL.
 * Phase 1: POST /upload/begin → get writeUrl
 * Phase 2: PUT to Azure (no backend in the middle)
 * Phase 3: POST /upload/{id}/commit → backend verifies & finalises
 */
export const uploadDirect = async (
  file: File,
  onProgress?: (pct: number) => void,
): Promise<FileItem> => {
  const { data: target } = await requestUploadTarget(file)

  // Client PUTs straight to Azure — no body through the backend
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

  // Backend verifies blob size (anti-forgery) and finalises the record
  const { data } = await client.post<FileItem>(`/files/upload/${target.uploadId}/commit`)
  return data
}

/** Convenience wrapper that matches the old uploadFile signature. */
export const uploadFile = (file: File, onProgress?: (pct: number) => void) =>
  uploadDirect(file, onProgress)
