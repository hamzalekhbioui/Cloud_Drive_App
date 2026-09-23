import { useEffect, useState } from 'react'
import AdminTable, { type AdminTableColumn } from '../components/AdminTable'
import { getAdminAudit, type AdminAuditLog, type AuditFilters } from '../api/audit'

export default function AdminAuditPage() {
  const [rows, setRows] = useState<AdminAuditLog[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [filters, setFilters] = useState<AuditFilters>({})
  const [expanded, setExpanded] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = () => {
    setLoading(true)
    setError(null)
    getAdminAudit(page, filters).then((response) => {
      setRows(response.data.content)
      setTotalPages(response.data.totalPages)
    }).catch(() => setError('Failed to load the audit log.')).finally(() => setLoading(false))
  }
  useEffect(load, [page, filters])

  const updateFilter = (key: keyof AuditFilters, value: string) => {
    setPage(0)
    setFilters((current) => ({ ...current, [key]: value || undefined }))
  }

  const columns: AdminTableColumn<AdminAuditLog>[] = [
    { key: 'created', header: 'Time', sortable: true, value: (row) => row.createdAt },
    { key: 'admin', header: 'Admin', sortable: true, value: (row) => row.adminEmail },
    { key: 'action', header: 'Action', sortable: true, value: (row) => row.action },
    { key: 'target', header: 'Target', value: (row) => `${row.targetType ?? '—'} ${row.targetId ?? ''}` },
    { key: 'ip', header: 'IP', value: (row) => row.ip ?? '—' },
    { key: 'detail', header: 'Details', render: (row) => <button className="btn" onClick={() => setExpanded((current) => current === row.id ? null : row.id)}>{expanded === row.id ? 'Hide JSON' : 'View JSON'}</button> },
  ]

  return <div className="page-inner">
    <div className="page-header"><div><div className="eyebrow">Administration</div><h1 className="title">Audit log</h1></div></div>
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
      <input className="input" placeholder="Admin email" onChange={(event) => updateFilter('admin', event.target.value)} />
      <input className="input" placeholder="Action" onChange={(event) => updateFilter('action', event.target.value)} />
      <input className="input" placeholder="Target type" onChange={(event) => updateFilter('targetType', event.target.value)} />
      <input className="input" type="date" aria-label="From date" onChange={(event) => updateFilter('fromDate', event.target.value)} />
      <input className="input" type="date" aria-label="To date" onChange={(event) => updateFilter('toDate', event.target.value)} />
    </div>
    <AdminTable columns={columns} data={rows} rowKey={(row) => String(row.id)} loading={loading} error={error} />
    {expanded !== null && rows.find((row) => row.id === expanded) && <pre className="card" style={{ whiteSpace: 'pre-wrap', overflow: 'auto', marginTop: 12 }}>{rows.find((row) => row.id === expanded)?.detailJson ?? '{}'} </pre>}
    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 12 }}><button className="btn" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button><span style={{ padding: 8, fontSize: 13 }}>Page {page + 1} of {totalPages}</span><button className="btn" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button></div>
  </div>
}
