import client from './client'

export interface FolderItem {
  id: number
  name: string
  userId: string
  teamId: number | null
  parentId: number | null
  createdAt: string
  updatedAt: string
}

export const getFolders = (teamId?: number) =>
  client.get<FolderItem[]>('/folders', { params: teamId === undefined ? {} : { teamId } })

export const createFolder = (name: string, teamId?: number, parentId?: number) =>
  client.post<FolderItem>('/folders', { name, teamId, parentId })

export const updateFolder = (folderId: number, name: string, parentId: number | null) =>
  client.patch<FolderItem>(`/folders/${folderId}`, { name, parentId })
