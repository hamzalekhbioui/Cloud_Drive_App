import { useEffect, useState } from 'react'
import { getSubscription, changePlan, type Subscription, type Plan } from '../api/subscriptions'
import Icon from '../components/Icon'
import { formatBytes } from '../utils/files'

const PLANS: { id: Plan; label: string; price: string; storage: number; features: string[] }[] = [
  {
    id: 'FREE',
    label: 'Free',
    price: '$0 / mo',
    storage: 5 * 1024 * 1024 * 1024,
    features: ['5 GB storage', 'File sharing', '100 req / min', 'Community support'],
  },
  {
    id: 'PRO',
    label: 'Pro',
    price: '$9.99 / mo',
    storage: 50 * 1024 * 1024 * 1024,
    features: ['50 GB storage', 'File sharing + public links', '500 req / min', 'Priority support'],
  },
  {
    id: 'BUSINESS',
    label: 'Business',
    price: '$29.99 / mo',
    storage: 1024 * 1024 * 1024 * 1024,
    features: ['1 TB storage', 'Teams & collaboration', '2 000 req / min', 'Dedicated support'],
  },
]

export default function PricingPage() {
  const [sub, setSub] = useState<Subscription | null>(null)
  const [loading, setLoading] = useState(true)
  const [upgrading, setUpgrading] = useState<Plan | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    getSubscription()
      .then(({ data }) => setSub(data))
      .catch(() => setError('Failed to load subscription.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleChangePlan(plan: Plan) {
    setError(''); setSuccess('')
    setUpgrading(plan)
    try {
      const { data } = await changePlan(plan)
      setSub(data)
      setSuccess(`Plan changed to ${plan} successfully.`)
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Failed to change plan.')
    } finally {
      setUpgrading(null)
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
      {success && (
        <div style={{ padding: 12, background: 'color-mix(in oklab, var(--success, #22c55e) 10%, var(--surface))', color: 'var(--success, #22c55e)', borderRadius: 10, marginBottom: 20, fontSize: 13 }}>
          {success}
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
        {PLANS.map((plan) => {
          const isCurrent = plan.id === currentPlan
          return (
            <div key={plan.id} style={{
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
                {plan.label}
              </div>
              <div style={{ fontFamily: 'var(--serif)', fontSize: 34, fontWeight: 700, marginBottom: 2 }}>{plan.price}</div>
              <div style={{ fontSize: 13, color: 'var(--ink-3)', marginBottom: 20 }}>{formatBytes(plan.storage)} storage</div>

              <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 24px', display: 'flex', flexDirection: 'column', gap: 8 }}>
                {plan.features.map((f) => (
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
                  onClick={() => handleChangePlan(plan.id)}
                  disabled={upgrading !== null}
                >
                  {upgrading === plan.id ? 'Changing…' : plan.id === 'FREE' ? 'Downgrade' : 'Upgrade'}
                </button>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}