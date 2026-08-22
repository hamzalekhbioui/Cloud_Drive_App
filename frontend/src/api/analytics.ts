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
  fileName: string
  size: number
  type: string
  createdAt: string | null
}

export interface ActivityItem {
  date: string
  size: number
  count: number
}

export interface Insight {
  type: 'info' | 'success' | 'warning' | 'tip'
  title: string
  message: string
}

export const getOverview = () => client.get<Overview>('/analytics/overview')
export const getBreakdown = () => client.get<BreakdownItem[]>('/analytics/breakdown')
export const getLargestFiles = () => client.get<LargestFile[]>('/analytics/largest-files')
export const getActivity = () => client.get<ActivityItem[]>('/analytics/activity')
export const getInsights = () => client.get<Insight[]>('/analytics/insights')
