import { useEffect, useState } from 'react'
import {
  getMyTeams, createTeam, inviteMember, removeMember, deleteTeam,
  acceptInvite, getPendingInvites,
  type Team, type TeamMember,
} from '../api/teams'
import { useAuth } from '../context/AuthContext'
import Icon from '../components/Icon'

export default function TeamsPage() {
  const { user } = useAuth()
  const [teams, setTeams] = useState<Team[]>([])
  const [pending, setPending] = useState<TeamMember[]>([])
  const [selected, setSelected] = useState<Team | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [newTeamName, setNewTeamName] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<'MEMBER' | 'ADMIN'>('MEMBER')
  const [inviting, setInviting] = useState(false)
  const [creating, setCreating] = useState(false)

  useEffect(() => { load() }, [])

  async function load() {
    setLoading(true)
    try {
      const [t, p] = await Promise.all([getMyTeams(), getPendingInvites()])
      setTeams(t.data)
      setPending(p.data)
      if (t.data.length > 0 && !selected) setSelected(t.data[0])
    } catch {
      setError('Failed to load teams.')
    } finally {
      setLoading(false)
    }
  }

  async function handleCreate() {
    if (!newTeamName.trim()) return
    setCreating(true)
    try {
      const { data } = await createTeam(newTeamName.trim())
      setTeams((prev) => [...prev, data])
      setSelected(data)
      setNewTeamName('')
      setShowCreate(false)
    } catch { setError('Failed to create team.') }
    finally { setCreating(false) }
  }

  async function handleInvite() {
    if (!selected || !inviteEmail.trim()) return
    setInviting(true)
    try {
      const { data } = await inviteMember(selected.id, inviteEmail.trim(), inviteRole)
      setSelected({ ...selected, members: [...selected.members, data] })
      setInviteEmail('')
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Failed to invite member.')
    } finally { setInviting(false) }
  }

  async function handleRemove(memberId: number) {
    if (!selected) return
    try {
      await removeMember(selected.id, memberId)
      setSelected({ ...selected, members: selected.members.filter((m) => m.id !== memberId) })
    } catch { setError('Failed to remove member.') }
  }

  async function handleDelete(teamId: number) {
    if (!confirm('Delete this team? This cannot be undone.')) return
    try {
      await deleteTeam(teamId)
      const next = teams.filter((t) => t.id !== teamId)
      setTeams(next)
      setSelected(next[0] ?? null)
    } catch { setError('Failed to delete team.') }
  }

  async function handleAccept(inviteToken: string) {
    try {
      await acceptInvite(inviteToken)
      await load()
    } catch { setError('Failed to accept invite.') }
  }

  if (loading) return <div className="page-inner"><div style={{ padding: 40, textAlign: 'center', color: 'var(--ink-3)' }}>Loading…</div></div>

  return (
    <div className="page-inner">
      <div className="page-header">
        <div>
          <div className="eyebrow">Collaboration</div>
          <h1 className="display">Teams</h1>
        </div>
        <button className="btn btn-accent" onClick={() => setShowCreate(true)}>
          <Icon name="plus" size={14} /> New team
        </button>
      </div>

      {error && (
        <div style={{ padding: 12, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 10, marginBottom: 20, fontSize: 13, display: 'flex', justifyContent: 'space-between' }}>
          {error}
          <button onClick={() => setError('')} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit' }}>×</button>
        </div>
      )}

      {/* Pending invites banner */}
      {pending.length > 0 && (
        <div style={{ marginBottom: 24, padding: 16, borderRadius: 12, background: 'color-mix(in oklab, var(--accent) 10%, var(--surface))', border: '1px solid color-mix(in oklab, var(--accent) 30%, var(--border))' }}>
          <div style={{ fontWeight: 600, marginBottom: 10, fontSize: 13 }}>Pending invites ({pending.length})</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {pending.map((inv) => (
              <div key={inv.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
                <span style={{ fontSize: 13 }}>Team invite as <strong>{inv.role}</strong></span>
                <button className="btn btn-accent" style={{ height: 28, fontSize: 12 }} onClick={() => handleAccept(inv.inviteToken!)}>
                  Accept
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {teams.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 80, color: 'var(--ink-3)' }}>
          <div style={{ fontFamily: 'var(--serif)', fontSize: 32, marginBottom: 8 }}>No teams yet.</div>
          <div>Create a team to collaborate with others.</div>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '220px 1fr', gap: 20, alignItems: 'start' }}>
          {/* Team list sidebar */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {teams.map((t) => (
              <button
                key={t.id}
                onClick={() => setSelected(t)}
                style={{
                  textAlign: 'left', padding: '10px 12px', borderRadius: 10,
                  background: selected?.id === t.id ? 'var(--surface-3)' : 'transparent',
                  border: selected?.id === t.id ? '1px solid var(--border)' : '1px solid transparent',
                  cursor: 'pointer', color: 'var(--ink)',
                }}
              >
                <div style={{ fontWeight: 600, fontSize: 14 }}>{t.name}</div>
                <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>{t.memberCount} members · {t.callerRole}</div>
              </button>
            ))}
          </div>

          {/* Team detail panel */}
          {selected && (
            <div style={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 14, padding: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: 20 }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: 18 }}>{selected.name}</div>
                  <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>
                    Owner: {selected.ownerEmail} · Created {new Date(selected.createdAt).toLocaleDateString()}
                  </div>
                </div>
                {selected.ownerEmail === user?.email && (
                  <button className="btn" style={{ height: 30, fontSize: 12, color: 'var(--danger)' }} onClick={() => handleDelete(selected.id)}>
                    <Icon name="trash" size={13} /> Delete team
                  </button>
                )}
              </div>

              {/* Invite form */}
              {(selected.callerRole === 'OWNER' || selected.callerRole === 'ADMIN') && (
                <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                  <input
                    placeholder="Email to invite…"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    style={{ flex: 1, height: 36, padding: '0 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--ink)', fontSize: 13 }}
                    onKeyDown={(e) => e.key === 'Enter' && handleInvite()}
                  />
                  <select
                    value={inviteRole}
                    onChange={(e) => setInviteRole(e.target.value as 'MEMBER' | 'ADMIN')}
                    style={{ height: 36, padding: '0 8px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface)', color: 'var(--ink)', fontSize: 13 }}
                  >
                    <option value="MEMBER">Member</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                  <button className="btn btn-accent" style={{ height: 36 }} onClick={handleInvite} disabled={inviting}>
                    <Icon name="mail" size={13} /> {inviting ? 'Sending…' : 'Invite'}
                  </button>
                </div>
              )}

              {/* Members list */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {selected.members.map((m) => (
                  <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', borderRadius: 8, background: 'var(--surface)' }}>
                    <div style={{ width: 30, height: 30, borderRadius: '50%', background: 'var(--accent)', display: 'grid', placeItems: 'center', color: '#fff', fontWeight: 700, fontSize: 13 }}>
                      {m.userEmail.charAt(0).toUpperCase()}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 13, fontWeight: 500 }}>{m.userEmail}</div>
                      <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>{m.role} · {m.status}</div>
                    </div>
                    {m.role !== 'OWNER' && (selected.callerRole === 'OWNER' || selected.callerRole === 'ADMIN' || m.userEmail === user?.email) && (
                      <button className="icon-btn" style={{ color: 'var(--danger)' }} onClick={() => handleRemove(m.id)} title="Remove">
                        <Icon name="close" size={14} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Create team modal */}
      {showCreate && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)', display: 'grid', placeItems: 'center', zIndex: 100 }}>
          <div style={{ background: 'var(--surface)', borderRadius: 16, padding: 28, width: 360, border: '1px solid var(--border)' }}>
            <div style={{ fontWeight: 700, fontSize: 16, marginBottom: 16 }}>Create a new team</div>
            <input
              autoFocus
              placeholder="Team name…"
              value={newTeamName}
              onChange={(e) => setNewTeamName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
              style={{ width: '100%', height: 38, padding: '0 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface-2)', color: 'var(--ink)', fontSize: 14, boxSizing: 'border-box' }}
            />
            <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
              <button className="btn" style={{ flex: 1 }} onClick={() => setShowCreate(false)}>Cancel</button>
              <button className="btn btn-accent" style={{ flex: 1 }} onClick={handleCreate} disabled={creating || !newTeamName.trim()}>
                {creating ? 'Creating…' : 'Create'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}