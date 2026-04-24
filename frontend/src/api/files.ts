import client from './client'

export interface FileItem {
  id: number
  originalFileName: string
  url: string
  size: number
  type: string
  createdAt: string
}

export const getMyFiles = () => client.get<FileItem[]>('/files/me')

export const uploadFile = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return client.post<FileItem>('/files/upload', form)
}

export const deleteFile = (fileId: number) => client.delete(`/files/${fileId}`)