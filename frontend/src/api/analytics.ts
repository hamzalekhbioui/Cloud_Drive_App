import client from './client'

export interface Overview {
  totalStorageUsed: number
  totalStorageLimit: number
  usagePercentage: number
  remainingStorage: number
  totalFiles: number
}

export interface BreakdownItem {
  category: string
  size: number
  percentage: number
  color: string
}

export interface LargestFile {
  id: number
  name: string
  size: number
  type: string
  createdAt: string
}

export interface ActivityItem {
  date: string
  totalUploadedSize: number
  fileCount: number
}

export interface Insight {
  type: 'info' | 'warning' | 'success' | 'tip'
  message: string
  detail: string
}

export const getOverview     = () => client.get<Overview>('/analytics/overview')
export const getBreakdown    = () => client.get<BreakdownItem[]>('/analytics/breakdown')
export const getLargestFiles = () => client.get<LargestFile[]>('/analytics/largest-files')
export const getActivity     = () => client.get<ActivityItem[]>('/analytics/activity')
export const getInsights     = () => client.get<Insight[]>('/analytics/insights')