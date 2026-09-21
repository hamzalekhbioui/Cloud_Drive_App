import { useEffect, useState } from 'react'
import AdminTable, { type AdminTableColumn } from '../components/AdminTable'
import { getAdminShares, revokeAdminShare, type AdminShare } from '../api/moderation'

export default function AdminSharesPage() {
  const [rows, setRows] = useState<AdminShare[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const load = () => { setLoading(true); getAdminShares(page).then((response) => { setRows(response.data.content); setTotalPages(response.data.totalPages) }).catch(() => setError('Failed to load shares.')).finally(() => setLoading(false)) }
  useEffect(load, [page])
  const columns: AdminTableColumn<AdminShare>[] = [
    { key: 'file', header: 'File', value: (row) => row.fileName ?? String(row.fileId) },
    { key: 'owner', header: 'Owner', sortable: true, value: (row) => row.ownerEmail },
    { key: 'recipient', header: 'Recipient', value: (row) => row.sharedWithEmail ?? 'Public' },
    { key: 'permission', header: 'Permission', value: (row) => row.permission },
    { key: 'status', header: 'Status', value: (row) => row.revokedAt ? 'REVOKED' : 'ACTIVE', render: (row) => row.revokedAt ? 'REVOKED' : <button className="btn" onClick={() => revokeAdminShare(row.id).then(load)}>Revoke</button> },
  ]
  return <div className="page-inner"><div className="page-header"><div><div className="eyebrow">Moderation</div><h1 className="title">Share links</h1></div></div><AdminTable columns={columns} data={rows} rowKey={(row) => String(row.id)} loading={loading} error={error} /><div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 12 }}><button className="btn" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Previous</button><span style={{ padding: 8, fontSize: 13 }}>Page {page + 1} of {totalPages}</span><button className="btn" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Next</button></div></div>
}
