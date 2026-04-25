import client from './client'

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

export const getMyFiles = () => client.get<FileItem[]>('/files/me')
export const getStarredFiles = () => client.get<FileItem[]>('/files/starred')
export const getTrashFiles = () => client.get<FileItem[]>('/files/trash')

export const uploadFile = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return client.post<FileItem>('/files/upload', form)
}

export const deleteFile = (fileId: number) => client.delete(`/files/${fileId}`)
export const restoreFile = (fileId: number) => client.post(`/files/${fileId}/restore`)
export const permanentlyDeleteFile = (fileId: number) => client.delete(`/files/${fileId}/permanent`)
export const starFile = (fileId: number) => client.patch<FileItem>(`/files/${fileId}/star`)
