import { useEffect, useState } from 'react'
import AdminTable, { type AdminTableColumn } from '../components/AdminTable'
import { getAdminPayments, getAdminSubscriptions, getAdminUsage, overrideAdminPlan, resetAdminUsage, type AdminPayment, type AdminSubscription, type AdminUsage, type BillingFilters } from '../api/billing'

type Tab = 'subscriptions' | 'payments' | 'usage'

export default function AdminBillingPage() {
  const [tab, setTab] = useState<Tab>('subscriptions')
  const [subscriptions, setSubscriptions] = useState<AdminSubscription[]>([])
  const [payments, setPayments] = useState<AdminPayment[]>([])
  const [usage, setUsage] = useState<AdminUsage[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [override, setOverride] = useState<AdminSubscription | null>(null)
  const [planId, setPlanId] = useState('')
  const [filters, setFilters] = useState<BillingFilters>({})

  const load = () => {
    setLoading(true)
    setError(null)
    const request = tab === 'subscriptions' ? getAdminSubscriptions(page, filters) : tab === 'payments' ? getAdminPayments(page, filters) : getAdminUsage(page, filters)
    request.then((response) => {
      setTotalPages(response.data.totalPages)
      if (tab === 'subscriptions') setSubscriptions(response.data.content as AdminSubscription[])
      if (tab === 'payments') setPayments(response.data.content as AdminPayment[])
      if (tab === 'usage') setUsage(response.data.content as AdminUsage[])
    }).catch(() => setError('Failed to load billing data.')).finally(() => setLoading(false))
  }
  useEffect(load, [tab, page, filters])
  const updateFilter = (key: keyof BillingFilters, value: string) => {
    setPage(0)
    setFilters((current) => ({ ...current, [key]: value || undefined }))
  }

  const subscriptionColumns: AdminTableColumn<AdminSubscription>[] = [
    { key: 'user', header: 'User', sortable: true, value: (row) => row.userEmail },
    { key: 'plan', header: 'Plan', sortable: true, value: (row) => row.plan },
    { key: 'status', header: 'Status', value: (row) => row.status },
    { key: 'period', header: 'Period end', value: (row) => row.currentPeriodEnd ?? '—' },
    { key: 'actions', header: 'Actions', render: (row) => <button className="btn" onClick={() => { setOverride(row); setPlanId(row.planId ? String(row.planId) : '') }}>Override plan</button> },
  ]
  const paymentColumns: AdminTableColumn<AdminPayment>[] = [
    { key: 'user', header: 'User', value: (row) => row.userEmail },
    { key: 'amount', header: 'Amount', value: (row) => `${(row.amountCents / 100).toFixed(2)} ${row.currency}` },
    { key: 'status', header: 'Status', value: (row) => row.status },
    { key: 'invoice', header: 'Invoice', value: (row) => row.stripeInvoiceId ?? '—' },
    { key: 'created', header: 'Created', value: (row) => row.createdAt ?? '—' },
  ]
  const usageColumns: AdminTableColumn<AdminUsage>[] = [
    { key: 'user', header: 'User', value: (row) => row.userEmail },
    { key: 'resource', header: 'Resource', value: (row) => row.resourceType },
    { key: 'period', header: 'Period', value: (row) => `${row.periodStart} – ${row.periodEnd}` },
    { key: 'count', header: 'Count', value: (row) => row.usageCount },
    { key: 'actions', header: 'Actions', render: (row) => <button className="btn" onClick={() => resetAdminUsage(row.userEmail).then(load)}>Reset user</button> },
  ]

  return <div className="page-inner">
    <div className="page-header"><div><div className="eyebrow">Administration</div><h1 className="title">Billing</h1></div></div>
    <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>{(['subscriptions', 'payments', 'usage'] as Tab[]).map((item) => <button key={item} className={`btn ${tab === item ? 'active' : ''}`} onClick={() => { setTab(item); setPage(0) }}>{item[0].toUpperCase() + item.slice(1)}</button>)}</div>
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
      {(tab !== 'usage') && <input className="input" placeholder="Status" onChange={(event) => updateFilter('status', event.target.value)} />}
      <input className="input" placeholder="Plan" onChange={(event) => updateFilter('plan', event.target.value)} />
      <input className="input" type="date" aria-label="From date" onChange={(event) => updateFilter('fromDate', event.target.value)} />
      <input className="input" type="date" aria-label="To date" onChange={(event) => updateFilter('toDate', event.target.value)} />
    </div>
    {tab === 'subscriptions' && <AdminTable columns={subscriptionColumns} data={subscriptions} rowKey={(row) => String(row.id)} loading={loading} error={error} />}
    {tab === 'payments' && <AdminTable columns={paymentColumns} data={payments} rowKey={(row) => String(row.id)} loading={loading} error={error} />}
    {tab === 'usage' && <AdminTable columns={usageColumns} data={usage} rowKey={(row) => String(row.id)} loading={loading} error={error} />}
    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 12 }}><button className="btn" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button><span style={{ padding: 8, fontSize: 13 }}>Page {page + 1} of {totalPages}</span><button className="btn" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button></div>
    {override && <div role="dialog" aria-modal="true" className="card" style={{ marginTop: 16, maxWidth: 420 }}><h2 style={{ marginTop: 0 }}>Override plan</h2><p style={{ color: 'var(--ink-3)' }}>{override.userEmail}</p><input className="input" type="number" min="1" value={planId} onChange={(event) => setPlanId(event.target.value)} placeholder="Plan ID" /><div style={{ display: 'flex', gap: 8, marginTop: 12 }}><button className="btn" onClick={() => overrideAdminPlan(override.userEmail, Number(planId)).then(() => { setOverride(null); load() })}>Save</button><button className="btn" onClick={() => setOverride(null)}>Cancel</button></div></div>}
  </div>
}
