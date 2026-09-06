import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import axios from 'axios'
import {
  createCheckoutSession, getPlans, getSubscription,
  type Plan, type PlanDefinition, type Subscription,
} from '../api/subscriptions'
import Icon from '../components/Icon'
import { formatBytes } from '../utils/files'

export default function PricingPage() {
  const [sub, setSub] = useState<Subscription | null>(null)
  const [plans, setPlans] = useState<PlanDefinition[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [checkoutPlan, setCheckoutPlan] = useState<Plan | null>(null)
  const [searchParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    const checkout = searchParams.get('checkout')
    if (checkout === 'success') setNotice('Checkout returned successfully. Payment status will update after Stripe confirms it.')
    if (checkout === 'cancelled') setNotice('Checkout was cancelled. No payment was confirmed.')
    if (checkout) setSearchParams({}, { replace: true })
    Promise.all([getSubscription(), getPlans()])
      .then(([subscriptionResponse, plansResponse]) => {
        setSub(subscriptionResponse.data)
        setPlans(plansResponse.data)
      })
      .catch(() => setError('Failed to load subscription.'))
      .finally(() => setLoading(false))
  }, [searchParams, setSearchParams])

  async function startCheckout(plan: Plan) {
    setCheckoutPlan(plan)
    setError('')
    try {
      const { data } = await createCheckoutSession(plan)
      window.location.assign(data.url)
    } catch (err: unknown) {
      const status = axios.isAxiosError(err) ? err.response?.status : undefined
      const message = axios.isAxiosError(err) ? err.response?.data?.message : undefined
      setError(status === 402
        ? 'This plan change requires an upgrade or payment action. Please try again from your billing settings.'
        : message || 'Unable to start checkout.')
      setCheckoutPlan(null)
    }
  }

  if (loading) return <div className="page-inner"><div style={{ padding: 40, textAlign: 'center', color: 'var(--ink-3)' }}>Loading…</div></div>

  const currentPlan = sub?.plan ?? 'FREE'

  return (
    <div className="page-inner">
      <div className="page-header">
        <div>
          <div className="eyebrow">Account</div>
          <h1 className="display">Plans & pricing</h1>
        </div>
      </div>

      {error && (
        <div style={{ padding: 12, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 10, marginBottom: 20, fontSize: 13 }}>
          {error}
        </div>
      )}
      {notice && (
        <div style={{ padding: 12, background: 'color-mix(in oklab, var(--info) 10%, var(--surface))', color: 'var(--info)', borderRadius: 10, marginBottom: 20, fontSize: 13 }}>
          {notice}
        </div>
      )}
      {sub && (
        <div style={{ marginBottom: 28, padding: 16, borderRadius: 12, background: 'var(--surface-2)', border: '1px solid var(--border)', fontSize: 13 }}>
          <strong>Current usage:</strong> {formatBytes(sub.storageUsedBytes)} of {formatBytes(sub.storageLimitBytes)} used ({sub.usagePercent.toFixed(1)}%)
          <div style={{ marginTop: 8, height: 6, borderRadius: 3, background: 'var(--surface-3)' }}>
            <div style={{ height: '100%', borderRadius: 3, background: 'var(--accent)', width: `${Math.min(sub.usagePercent, 100)}%`, transition: 'width .4s' }} />
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 20 }}>
        {plans.map((plan) => {
          const isCurrent = plan.slug === currentPlan
          const price = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: plan.currency,
          }).format(plan.priceCents / 100)
          const interval = plan.billingInterval.toLowerCase()
          return (
            <div key={plan.slug} style={{
              borderRadius: 16, padding: 24,
              border: isCurrent ? '2px solid var(--accent)' : '1px solid var(--border)',
              background: 'var(--surface-2)',
              position: 'relative',
            }}>
              {isCurrent && (
                <div style={{ position: 'absolute', top: -1, right: 20, background: 'var(--accent)', color: '#fff', fontSize: 11, fontWeight: 700, padding: '3px 10px', borderRadius: '0 0 8px 8px' }}>
                  CURRENT PLAN
                </div>
              )}

              <div style={{ marginBottom: 4, fontSize: 11, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '.06em', color: 'var(--ink-3)' }}>
                {plan.name}
              </div>
              <div style={{ fontFamily: 'var(--serif)', fontSize: 34, fontWeight: 700, marginBottom: 2 }}>{price} / {interval}</div>
              <div style={{ fontSize: 13, color: 'var(--ink-3)', marginBottom: 20 }}>{formatBytes(plan.storageLimitBytes)} storage</div>

              <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 24px', display: 'flex', flexDirection: 'column', gap: 8 }}>
                {[
                  `${formatBytes(plan.storageLimitBytes)} storage`,
                  `${formatBytes(plan.maxFileSizeBytes)} max file size`,
                  `${plan.rateLimitPerMinute.toLocaleString()} req / min`,
                  plan.maxTeams < 0 ? 'Unlimited teams' : `${plan.maxTeams} teams`,
                ].map((f) => (
                  <li key={f} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}>
                    <Icon name="check" size={14} style={{ color: 'var(--accent)', flexShrink: 0 }} />
                    {f}
                  </li>
                ))}
              </ul>

              {isCurrent ? (
                <button className="btn" disabled style={{ width: '100%' }}>Active plan</button>
              ) : (
                <button
                  className="btn btn-accent"
                  style={{ width: '100%' }}
                  disabled={checkoutPlan === plan.slug}
                  onClick={() => void startCheckout(plan.slug)}
                >
                  {checkoutPlan === plan.slug ? 'Opening checkout…' : 'Choose plan'}
                </button>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}