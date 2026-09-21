import adminClient from './adminClient'

export interface AdminPlanCount {
  plan: string
  count: number
}

export interface AdminStatusCount {
  status: string
  count: number
}

export interface AdminOverview {
  totalUsers: number
  activeUsers: number
  disabledUsers: number
  totalFiles: number
  trashedFiles: number
  bytesStored: number
  activeShares: number
  publicShares: number
  privateShares: number
  teams: number
  activeSubscriptions: AdminPlanCount[]
  mrrCents: number
  aiProcessing: AdminStatusCount[]
}

export interface AdminGrowthPoint {
  date: string
  signups: number
  uploads: number
  uploadedBytes: number
}

export interface AdminStoragePoint {
  category: string
  bytes: number
}

export interface AdminStorageOverview {
  byPlan: AdminStoragePoint[]
  byFileType: AdminStoragePoint[]
}

export const getAdminOverview = () => adminClient.get<AdminOverview>('/overview')
export const getAdminGrowth = () => adminClient.get<AdminGrowthPoint[]>('/overview/growth')
export const getAdminStorage = () => adminClient.get<AdminStorageOverview>('/overview/storage')
