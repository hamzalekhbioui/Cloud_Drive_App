import { useEffect, useState } from 'react'
import AdminTable, { type AdminTableColumn } from '../components/AdminTable'
import { getAdminTeams, type AdminTeam } from '../api/moderation'

export default function AdminTeamsPage() {
  const [rows, setRows] = useState<AdminTeam[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => { getAdminTeams().then((response) => setRows(response.data.content)).catch(() => setError('Failed to load teams.')).finally(() => setLoading(false)) }, [])
  const columns: AdminTableColumn<AdminTeam>[] = [
    { key: 'name', header: 'Team', sortable: true, value: (row) => row.name },
    { key: 'owner', header: 'Owner', sortable: true, value: (row) => row.ownerEmail },
    { key: 'members', header: 'Members', sortable: true, value: (row) => row.members.length },
    { key: 'status', header: 'Membership status', render: (row) => row.members.map((member) => `${member.userEmail} (${member.status})`).join(', ') || 'No members' },
  ]
  return <div className="page-inner"><div className="page-header"><div><div className="eyebrow">Administration</div><h1 className="title">Teams</h1></div></div><AdminTable columns={columns} data={rows} rowKey={(row) => String(row.id)} loading={loading} error={error} /></div>
}
