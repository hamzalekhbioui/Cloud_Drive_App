import { useEffect, useState } from 'react'
import AdminTable, { type AdminTableColumn } from '../components/AdminTable'
import { deleteAdminFile, getAdminFiles, restoreAdminFile, type AdminFile } from '../api/moderation'
import { formatBytes } from '../../utils/files'

export default function AdminFilesPage() {
  const [rows, setRows] = useState<AdminFile[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const load = () => { setLoading(true); getAdminFiles(page).then((response) => { setRows(response.data.content); setTotalPages(response.data.totalPages) }).catch(() => setError('Failed to load files.')).finally(() => setLoading(false)) }
  useEffect(load, [page])
  const columns: AdminTableColumn<AdminFile>[] = [
    { key: 'fileName', header: 'File', sortable: true, value: (row) => row.fileName },
    { key: 'owner', header: 'Owner', sortable: true, value: (row) => row.ownerEmail },
    { key: 'type', header: 'Type', value: (row) => row.type },
    { key: 'size', header: 'Size', sortable: true, value: (row) => row.size, render: (row) => formatBytes(row.size) },
    { key: 'status', header: 'Status', value: (row) => row.deletedAt ? 'TRASHED' : row.status, render: (row) => row.deletedAt ? 'TRASHED' : row.status },
    { key: 'actions', header: 'Actions', render: (row) => <button className="btn" onClick={() => (row.deletedAt ? restoreAdminFile(row.id) : deleteAdminFile(row.id)).then(load)}> {row.deletedAt ? 'Restore' : 'Delete'} </button> },
  ]
  return <div className="page-inner"><div className="page-header"><div><div className="eyebrow">Moderation</div><h1 className="title">Files</h1></div></div><AdminTable columns={columns} data={rows} rowKey={(row) => String(row.id)} loading={loading} error={error} /><div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 12 }}><button className="btn" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button><span style={{ padding: 8, fontSize: 13 }}>Page {page + 1} of {totalPages}</span><button className="btn" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button></div></div>
}
