import client from './client'

export type Plan = 'FREE' | 'PRO' | 'BUSINESS'
export type SubscriptionStatus =
  | 'ACTIVE'
  | 'PAST_DUE'
  | 'CANCELLED'
  | 'INCOMPLETE'
  | 'INCOMPLETE_EXPIRED'
  | 'TRIALING'
  | 'UNPAID'
  | 'PAUSED'

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
  status: SubscriptionStatus
  storageLimitBytes: number
  storageUsedBytes: number
  usagePercent: number
  startDate: string
  endDate: string | null
  billingInterval: 'MONTH' | 'YEAR'
  currentPeriodStart: string | null
  currentPeriodEnd: string | null
  cancelAtPeriodEnd: boolean
}

export interface Usage {
  storageLimitBytes: number
  storageUsedBytes: number
  usagePercent: number
  aiQueriesUsed: number
  aiQueriesLimit: number
  periodStart: string
  periodEnd: string
}

export const getSubscription = () =>
  client.get<Subscription>('/subscriptions')

export const getPlans = () =>
  client.get<PlanDefinition[]>('/plans')

export const createCheckoutSession = (plan: Plan) =>
  client.post<{ id: string; url: string }>('/billing/checkout', { plan })

export const cancelSubscription = () =>
  client.post<Subscription>('/billing/cancel')

export const reactivateSubscription = () =>
  client.post<Subscription>('/billing/reactivate')

export const createPortalSession = () =>
  client.post<{ id: string; url: string }>('/billing/portal')

export const getUsage = () =>
  client.get<Usage>('/billing/usage')