import client from './client'

export interface SettingsData {
  // profile
  name: string
  email: string
  memberSince: string | null
  lastLogin: string | null
  hasPassword: boolean
  // storage
  storageUsed: number
  storageTotal: number
  storagePercentage: number
  totalFiles: number
  // preferences
  darkMode: boolean
  density: 'compact' | 'comfortable' | 'airy'
  emailNotificationsEnabled: boolean
  storageWarningEnabled: boolean
  uploadNotifications: boolean
  deleteNotifications: boolean
  inAppNotifications: boolean
  defaultView: 'grid' | 'list'
  defaultSort: 'name' | 'size' | 'date'
  autoOrganize: boolean
  debugMode: boolean
  apiToken: string | null
}

export type PreferencesUpdate = Partial<{
  darkMode: boolean
  density: string
  emailNotificationsEnabled: boolean
  storageWarningEnabled: boolean
  uploadNotifications: boolean
  deleteNotifications: boolean
  inAppNotifications: boolean
  defaultView: string
  defaultSort: string
  autoOrganize: boolean
  debugMode: boolean
}>

export const getSettings        = ()                           => client.get<SettingsData>('/settings')
export const updatePreferences  = (data: PreferencesUpdate)   => client.put('/settings/preferences', data)
export const updateProfile      = (name: string)              => client.put('/settings/profile', { name })
export const updatePassword     = (currentPassword: string, newPassword: string) =>
  client.put('/settings/password', { currentPassword, newPassword })
export const regenerateApiToken = ()                           => client.post<{ token: string }>('/settings/api-token')