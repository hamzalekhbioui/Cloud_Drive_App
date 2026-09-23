import { useEffect, useState } from 'react'
import AdminTable, { type AdminTableColumn } from '../components/AdminTable'
import { deleteAdminFile, getAdminFiles, purgeAdminFile, restoreAdminFile, type AdminFile, type AdminFileFilters } from '../api/moderation'
import { formatBytes } from '../../utils/files'

export default function AdminFilesPage() {
  const [rows, setRows] = useState<AdminFile[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [filters, setFilters] = useState<AdminFileFilters>({})
  const load = () => {
    setLoading(true)
    setError(null)
    getAdminFiles(page, filters).then((response) => {
      setRows(response.data.content)
      setTotalPages(response.data.totalPages)
    }).catch(() => setError('Failed to load files.')).finally(() => setLoading(false))
  }
  useEffect(load, [page, filters])
  const updateFilter = (key: keyof AdminFileFilters, value: string) => {
    setPage(0)
    const normalized = key === 'minSize' || key === 'maxSize'
      ? (value ? Number(value) : undefined)
      : (value || undefined)
    setFilters((current) => ({ ...current, [key]: normalized }))
  }
  const columns: AdminTableColumn<AdminFile>[] = [
    { key: 'fileName', header: 'File', sortable: true, value: (row) => row.fileName },
    { key: 'owner', header: 'Owner', sortable: true, value: (row) => row.ownerEmail },
    { key: 'type', header: 'Type', value: (row) => row.type },
    { key: 'size', header: 'Size', sortable: true, value: (row) => row.size, render: (row) => formatBytes(row.size) },
    { key: 'status', header: 'Status', value: (row) => row.deletedAt ? 'TRASHED' : row.status, render: (row) => row.deletedAt ? 'TRASHED' : row.status },
    { key: 'team', header: 'Team', value: (row) => row.teamName ?? '—' },
    { key: 'actions', header: 'Actions', render: (row) => <div style={{ display: 'flex', gap: 6 }}>
      <button className="btn" onClick={() => (row.deletedAt ? restoreAdminFile(row.id) : deleteAdminFile(row.id)).then(load)}>{row.deletedAt ? 'Restore' : 'Delete'}</button>
      {row.deletedAt && <button className="btn" onClick={() => purgeAdminFile(row.id).then(load)}>Purge</button>}
    </div> },
  ]
  return <div className="page-inner"><div className="page-header"><div><div className="eyebrow">Moderation</div><h1 className="title">Files</h1></div></div>
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
      <input className="input" placeholder="Owner" onChange={(event) => updateFilter('owner', event.target.value)} />
      <input className="input" placeholder="Status" onChange={(event) => updateFilter('status', event.target.value)} />
      <input className="input" placeholder="Type" onChange={(event) => updateFilter('type', event.target.value)} />
      <input className="input" type="number" min="0" placeholder="Min bytes" onChange={(event) => updateFilter('minSize', event.target.value)} />
      <input className="input" type="number" min="0" placeholder="Max bytes" onChange={(event) => updateFilter('maxSize', event.target.value)} />
      <input className="input" type="date" aria-label="From date" onChange={(event) => updateFilter('fromDate', event.target.value)} />
      <input className="input" type="date" aria-label="To date" onChange={(event) => updateFilter('toDate', event.target.value)} />
    </div>
    <AdminTable columns={columns} data={rows} rowKey={(row) => String(row.id)} loading={loading} error={error} /><div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 12 }}><button className="btn" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button><span style={{ padding: 8, fontSize: 13 }}>Page {page + 1} of {totalPages}</span><button className="btn" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button></div></div>
}
