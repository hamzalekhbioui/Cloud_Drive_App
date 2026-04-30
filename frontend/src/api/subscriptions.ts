import client from './client'

export type Plan = 'FREE' | 'PRO' | 'BUSINESS'

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

export const changePlan = (plan: Plan) =>
  client.put<Subscription>('/subscriptions/plan', { plan })