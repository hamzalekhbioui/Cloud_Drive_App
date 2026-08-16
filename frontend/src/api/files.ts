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

/** Legacy upload via backend proxy (multipart). */
export const uploadMultipart = async (
  file: File,
  onProgress?: (pct: number) => void
): Promise<FileItem> => {
  const formData = new FormData()
  formData.append('file', file)
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
  onProgress?: (pct: number) => void
): Promise<FileItem> => {
  try {
    const { data: target } = await requestUploadTarget(file)

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
  } catch (err: any) {
    // If it's a CORS error (no response) or Azure-specific error, try the legacy multipart upload
    const isNetworkOrCorsError = !err.response && err.request
    const isAzureError = err.config?.url?.includes('blob.core.windows.net')

    if (isNetworkOrCorsError || isAzureError) {
      console.warn('Direct upload failed (possibly CORS). Falling back to multipart proxy upload.', err)
      return uploadMultipart(file, onProgress)
    }
    throw err
  }
}
