import adminClient from './adminClient'

export interface AdminPage<T> {
  content: T[]
  totalPages: number
  number: number
}

export interface AdminSubscription {
  id: number
  userEmail: string
  planId: number | null
  plan: string
  status: string
  usedBytes: number
  startDate: string | null
  currentPeriodEnd: string | null
  cancelAtPeriodEnd: boolean
}

export interface AdminPayment {
  id: number
  userEmail: string
  amountCents: number
  currency: string
  status: string
  stripeInvoiceId: string | null
  createdAt: string | null
}

export interface AdminUsage {
  id: number
  userEmail: string
  resourceType: string
  periodStart: string
  periodEnd: string
  usageCount: number
}

export interface AdminWebhookEvent {
  id: number
  stripeEventId: string
  eventType: string
  payload: string
  processed: boolean
  processingError: string | null
  createdAt: string | null
  processedAt: string | null
}

export interface BillingFilters {
  status?: string
  plan?: string
  fromDate?: string
  toDate?: string
}

export const getAdminSubscriptions = (page = 0, filters: BillingFilters = {}) => adminClient.get<AdminPage<AdminSubscription>>('/billing/subscriptions', { params: { page, ...filters } })
export const getAdminPayments = (page = 0, filters: BillingFilters = {}) => adminClient.get<AdminPage<AdminPayment>>('/billing/payments', { params: { page, ...filters } })
export const getAdminUsage = (page = 0, filters: BillingFilters = {}) => adminClient.get<AdminPage<AdminUsage>>('/billing/usage', { params: { page, ...filters } })
export const overrideAdminPlan = (userId: string, planId: number) => adminClient.post<AdminSubscription>(`/billing/subscriptions/${encodeURIComponent(userId)}/plan`, { planId })
export const resetAdminUsage = (userEmail: string) => adminClient.post(`/billing/usage/${encodeURIComponent(userEmail)}/reset`)
export const getAdminWebhooks = (page = 0) => adminClient.get<AdminPage<AdminWebhookEvent>>('/webhooks', { params: { page } })
export const replayAdminWebhook = (id: number) => adminClient.post<AdminWebhookEvent>(`/webhooks/${id}/replay`)
