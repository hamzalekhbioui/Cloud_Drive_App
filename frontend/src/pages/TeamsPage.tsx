import { useEffect, useState } from 'react'
import {
  getMyTeams, createTeam, inviteMember, removeMember, deleteTeam,
  acceptInvite, declineInvite, getPendingInvites,
  type Team, type TeamMember,
} from '../api/teams'
import { getTeamFiles, uploadFile, type FileItem } from '../api/files'
import { useAuth } from '../context/AuthContext'
import Icon from '../components/Icon'
import { formatBytes } from '../utils/files'
import FilePreviewModal from '../components/FilePreviewModal'

export default function TeamsPage() {
  const { user } = useAuth()
  const [teams, setTeams] = useState<Team[]>([])
  const [pending, setPending] = useState<TeamMember[]>([])
  const [selected, setSelected] = useState<Team | null>(null)
  const [teamFiles, setTeamFiles] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingFiles, setLoadingFiles] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [newTeamName, setNewTeamName] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<'MEMBER' | 'ADMIN'>('MEMBER')
  const [inviting, setInviting] = useState(false)
  const [creating, setCreating] = useState(false)
  const [previewFile, setPreviewFile] = useState<FileItem | null>(null)
  const [teamPendingDelete, setTeamPendingDelete] = useState<Team | null>(null)

  useEffect(() => { load() }, [])

  useEffect(() => {
    if (selected) {
      loadTeamFiles(selected.id)
    } else {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setTeamFiles([])
    }
  }, [selected])

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

  async function loadTeamFiles(teamId: number) {
    setLoadingFiles(true)
    try {
      const { data } = await getTeamFiles(teamId)
      setTeamFiles(data)
    } catch {
      setError('Failed to load team files.')
    } finally {
      setLoadingFiles(false)
    }
  }

  async function handleFileUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file || !selected) return
    setUploading(true)
    try {
      const newFile = await uploadFile(file, undefined, selected.id)
      setTeamFiles((prev) => [newFile, ...prev])
    } catch (err: unknown) {
      const response = (err as { response?: { status?: number; data?: { message?: string } } }).response
      setError(response?.status === 413
        ? 'This file exceeds your plan’s maximum upload size. Upgrade your plan to upload larger files.'
        : response?.data?.message || 'Upload failed.')
    } finally {
      setUploading(false)
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
    } catch (err: unknown) {
      const response = (err as { response?: { status?: number; data?: { message?: string } } }).response
      setError(response?.status === 402
        ? 'You have reached your plan’s team limit. Upgrade your plan to create another team.'
        : response?.data?.message || 'Failed to create team.')
    }
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
      const response = (e as { response?: { status?: number; data?: { message?: string } } }).response
      setError(response?.status === 402
        ? 'You have reached your plan’s member limit. Upgrade your plan to invite another member.'
        : response?.data?.message || 'Failed to invite member.')
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
    try {
      await deleteTeam(teamId)
      const next = teams.filter((t) => t.id !== teamId)
      setTeams(next)
      setSelected(next[0] ?? null)
      setTeamPendingDelete(null)
    } catch { setError('Failed to delete team.') }
  }

  async function handleAccept(inviteToken: string) {
    try {
      await acceptInvite(inviteToken)
      await load()
    } catch { setError('Failed to accept invite.') }
  }

  async function handleDecline(inviteToken: string) {
    try {
      await declineInvite(inviteToken)
      await load()
    } catch { setError('Failed to decline invite.') }
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
        <div style={{
          marginBottom: 32,
          padding: 24,
          borderRadius: 16,
          background: 'var(--ink)',
          color: 'var(--bg)',
          boxShadow: '0 20px 40px -10px color-mix(in oklab, var(--ink) 30%, transparent)',
          position: 'relative',
          overflow: 'hidden'
        }}>
          <div style={{
            position: 'absolute',
            top: -20, right: -20,
            width: 100, height: 100,
            background: 'var(--accent)',
            filter: 'blur(40px)',
            opacity: 0.3
          }} />
          <div style={{
            fontSize: 11,
            textTransform: 'uppercase',
            letterSpacing: '0.1em',
            fontWeight: 700,
            opacity: 0.6,
            marginBottom: 12,
            display: 'flex',
            alignItems: 'center',
            gap: 6
          }}>
            <Icon name="mail" size={12} /> Pending Invitations
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {pending.map((inv) => (
              <div key={inv.id} style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '16px 20px',
                borderRadius: 12,
                background: 'rgba(255,255,255,0.05)',
                border: '1px solid rgba(255,255,255,0.1)'
              }}>
                <div>
                  <div style={{ fontSize: 15, fontWeight: 500 }}>
                    You've been invited to a team
                  </div>
                  <div style={{ fontSize: 13, opacity: 0.6, marginTop: 2 }}>
                    Role: <span style={{ textTransform: 'capitalize' }}>{inv.role.toLowerCase()}</span>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button
                    className="btn btn-accent"
                    style={{ height: 36, padding: '0 20px', fontSize: 13 }}
                    onClick={() => handleAccept(inv.inviteToken!)}
                  >
                    Accept
                  </button>
                  <button
                    className="btn"
                    style={{ height: 36, padding: '0 20px', fontSize: 13, background: 'rgba(255,255,255,0.1)', color: '#fff', border: 'none' }}
                    onClick={() => handleDecline(inv.inviteToken!)}
                  >
                    Decline
                  </button>
                </div>
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
              <div key={t.id} style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <button
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
                {selected?.id === t.id && (
                  <div style={{ padding: '4px 12px 12px 24px', display: 'flex', flexDirection: 'column', gap: 6 }}>
                    {t.members.filter(m => m.status === 'ACTIVE').map(m => (
                      <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, color: 'var(--ink-2)' }}>
                        <div style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--accent)', opacity: 0.6 }} />
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {m.userEmail === user?.email ? 'You' : m.userEmail.split('@')[0]}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
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
                  <button
                    className="btn"
                    style={{ height: 30, fontSize: 12, color: 'var(--danger)' }}
                    onClick={() => setTeamPendingDelete(selected)}
                  >
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
              <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 12, color: 'var(--ink-3)' }}>Members</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 32 }}>
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

              {/* Team Files section */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--ink-3)' }}>Shared Files</div>
                <label className="btn btn-accent" style={{ height: 28, fontSize: 12, cursor: 'pointer' }}>
                  <Icon name="plus" size={12} /> Upload to Team
                  <input type="file" style={{ display: 'none' }} onChange={handleFileUpload} disabled={uploading} />
                </label>
              </div>

              {loadingFiles ? (
                <div style={{ padding: '20px 0', textAlign: 'center', fontSize: 13, color: 'var(--ink-4)' }}>Loading files…</div>
              ) : teamFiles.length === 0 ? (
                <div style={{ padding: '40px 0', textAlign: 'center', fontSize: 13, color: 'var(--ink-4)', background: 'var(--surface)', borderRadius: 10, border: '1px dashed var(--border)' }}>
                  No files shared with this team yet.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  {teamFiles.map((f) => (
                    <div key={f.id} style={{
                      display: 'flex', alignItems: 'center', gap: 12, padding: '10px 12px',
                      borderRadius: 8, border: '1px solid transparent', transition: 'all 0.2s'
                    }} className="hover-reveal">
                      <div style={{ width: 32, height: 32, borderRadius: 6, background: 'var(--surface-3)', display: 'grid', placeItems: 'center', color: 'var(--ink-3)' }}>
                        <Icon name="files" size={16} />
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 13, fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {f.originalFileName}
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--ink-4)' }}>
                          {formatBytes(f.size)} · {new Date(f.createdAt).toLocaleDateString()} · by {f.userId === user?.email ? 'You' : f.userId.split('@')[0]}
                        </div>
                      </div>
                      <button
                        type="button"
                        className="btn btn-accent"
                        style={{ height: 28, padding: '0 12px', fontSize: 12 }}
                        onClick={() => setPreviewFile(f)}
                      >
                        View
                      </button>
                      <a
                        href={f.url}
                        target="_blank"
                        rel="noreferrer"
                        className="btn"
                        style={{ height: 28, padding: '0 12px', fontSize: 12 }}
                      >
                        Download
                      </a>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {previewFile && (
        <FilePreviewModal
          file={previewFile}
          onClose={() => setPreviewFile(null)}
        />
      )}

      {teamPendingDelete && (
        <div className="modal-backdrop" onClick={() => setTeamPendingDelete(null)}>
          <div
            className="modal"
            style={{ maxWidth: 460 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-head">
              <h2 className="modal-title" style={{ fontSize: 22 }}>Delete team?</h2>
              <button className="icon-btn" onClick={() => setTeamPendingDelete(null)} aria-label="Close">
                <Icon name="close" size={16} />
              </button>
            </div>
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              <div style={{ fontSize: 14, color: 'var(--ink-2)', lineHeight: 1.6 }}>
                You are about to permanently delete
                {' '}
                <strong style={{ color: 'var(--ink)' }}>{teamPendingDelete.name}</strong>.
              </div>
              <div style={{ fontSize: 13, color: 'var(--danger)', lineHeight: 1.6 }}>
                This action cannot be undone.
              </div>
            </div>
            <div className="modal-foot" style={{ justifyContent: 'flex-end' }}>
              <button className="btn" onClick={() => setTeamPendingDelete(null)}>
                Cancel
              </button>
              <button className="btn btn-danger" onClick={() => handleDelete(teamPendingDelete.id)}>
                <Icon name="trash" size={14} /> Delete team
              </button>
            </div>
          </div>
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