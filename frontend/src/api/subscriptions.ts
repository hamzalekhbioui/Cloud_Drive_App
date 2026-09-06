import client from './client'

export type Plan = 'FREE' | 'PRO' | 'BUSINESS'

export interface PlanDefinition {
  name: string
  slug: Plan
  storageLimitBytes: number
  maxFileSizeBytes: number
  maxTeams: number
  maxTeamMembers: number
  aiQueriesPerMonth: number
  rateLimitPerMinute: number
  priceCents: number
  currency: string
  billingInterval: string
}

export interface Subscription {
  plan: Plan
  status: string
  storageLimitBytes: number
  storageUsedBytes: number
  usagePercent: number
  startDate: string
  endDate: string | null
}

export const getSubscription = () =>
  client.get<Subscription>('/subscriptions')

export const getPlans = () =>
  client.get<PlanDefinition[]>('/plans')