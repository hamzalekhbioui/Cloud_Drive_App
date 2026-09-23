import { useEffect, useState } from 'react'
import AdminTable, { type AdminTableColumn } from '../components/AdminTable'
import { getAdminWebhooks, replayAdminWebhook, type AdminWebhookEvent } from '../api/billing'

export default function AdminWebhooksPage() {
  const [rows, setRows] = useState<AdminWebhookEvent[]>([])
  const [selected, setSelected] = useState<AdminWebhookEvent | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const load = () => { setLoading(true); getAdminWebhooks(page).then((response) => { setRows(response.data.content); setTotalPages(response.data.totalPages) }).catch(() => setError('Failed to load webhook events.')).finally(() => setLoading(false)) }
  useEffect(load, [page])
  const columns: AdminTableColumn<AdminWebhookEvent>[] = [
    { key: 'type', header: 'Event', sortable: true, value: (row) => row.eventType },
    { key: 'stripeId', header: 'Stripe ID', value: (row) => row.stripeEventId },
    { key: 'status', header: 'Status', value: (row) => row.processed ? 'PROCESSED' : 'FAILED' },
    { key: 'created', header: 'Created', value: (row) => row.createdAt ?? '—' },
    { key: 'actions', header: 'Actions', render: (row) => <div style={{ display: 'flex', gap: 6 }}><button className="btn" onClick={() => setSelected(row)}>Payload</button>{!row.processed && <button className="btn" onClick={() => replayAdminWebhook(row.id).then(load)}>Replay</button>}</div> },
  ]
  return <div className="page-inner"><div className="page-header"><div><div className="eyebrow">Administration</div><h1 className="title">Stripe webhooks</h1></div></div><AdminTable columns={columns} data={rows} rowKey={(row) => String(row.id)} loading={loading} error={error} /><div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 12 }}><button className="btn" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button><span style={{ padding: 8, fontSize: 13 }}>Page {page + 1} of {totalPages}</span><button className="btn" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button></div>{selected && <div role="dialog" className="card" style={{ marginTop: 16 }}><div style={{ display: 'flex', justifyContent: 'space-between' }}><h2 style={{ marginTop: 0 }}>Payload</h2><button className="btn" onClick={() => setSelected(null)}>Close</button></div><pre style={{ whiteSpace: 'pre-wrap', overflow: 'auto', maxHeight: 420 }}>{selected.payload}</pre>{selected.processingError && <div style={{ color: 'var(--danger)' }}>{selected.processingError}</div>}</div>}</div>
}
