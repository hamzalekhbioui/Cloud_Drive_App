import { useEffect, useState } from 'react'
import { createShare, getSharesForFile, revokeShare, type ShareItem } from '../api/shares'
import Icon from '../components/Icon'

interface Props {
  fileId: number
  fileName: string
  onClose: () => void
}

export default function ShareModal({ fileId, fileName, onClose }: Props) {
  const [shares, setShares] = useState<ShareItem[]>([])
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState<'user' | 'public'>('user')
  const [email, setEmail] = useState('')
  const [permission, setPermission] = useState<'VIEW' | 'DOWNLOAD'>('VIEW')
  const [expiresAt, setExpiresAt] = useState('')
  const [sharing, setSharing] = useState(false)
  const [error, setError] = useState('')
  const [copied, setCopied] = useState<number | null>(null)

  useEffect(() => {
    getSharesForFile(fileId)
      .then(({ data }) => setShares(data))
      .finally(() => setLoading(false))
  }, [fileId])

  async function handleShare() {
    setError('')
    if (tab === 'user' && !email.trim()) { setError('Enter an email address.'); return }
    setSharing(true)
    try {
      const { data } = await createShare(fileId, {
        sharedWithEmail: tab === 'user' ? email.trim() : null,
        permission,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null,
      })
      setShares((prev) => [...prev, data])
      setEmail(''); setExpiresAt('')
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Failed to create share.')
    } finally {
      setSharing(false)
    }
  }

  async function handleRevoke(shareId: number) {
    try {
      await revokeShare(shareId)
      setShares((prev) => prev.filter((s) => s.id !== shareId))
    } catch { setError('Failed to revoke share.') }
  }

  function copyLink(share: ShareItem) {
    const url = `${window.location.origin}/api/shares/public/${share.token}/stream`
    navigator.clipboard.writeText(url)
    setCopied(share.id)
    setTimeout(() => setCopied(null), 2000)
  }

  const userShares = shares.filter((s) => s.sharedWithEmail !== null)
  const publicShares = shares.filter((s) => s.sharedWithEmail === null)

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)', display: 'grid', placeItems: 'center', zIndex: 200 }} onClick={onClose}>
      <div style={{ background: 'var(--surface)', borderRadius: 18, width: 460, maxWidth: '95vw', border: '1px solid var(--border)', overflow: 'hidden' }} onClick={(e) => e.stopPropagation()}>

        {/* Header */}
        <div style={{ padding: '18px 20px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontWeight: 700, fontSize: 15 }}>Share file</div>
            <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 340 }}>{fileName}</div>
          </div>
          <button className="icon-btn" onClick={onClose}><Icon name="close" size={16} /></button>
        </div>

        {/* Tabs */}
        <div style={{ display: 'flex', gap: 0, padding: '14px 20px 0', borderBottom: '1px solid var(--border)' }}>
          {(['user', 'public'] as const).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              style={{
                padding: '6px 14px', background: 'none', border: 'none', cursor: 'pointer',
                fontWeight: tab === t ? 600 : 400, fontSize: 13,
                color: tab === t ? 'var(--ink)' : 'var(--ink-3)',
                borderBottom: tab === t ? '2px solid var(--accent)' : '2px solid transparent',
                marginBottom: -1,
              }}
            >
              {t === 'user' ? 'Share with user' : 'Public link'}
            </button>
          ))}
        </div>

        <div style={{ padding: 20 }}>
          {error && <div style={{ padding: 10, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 8, marginBottom: 14, fontSize: 12 }}>{error}</div>}

          {/* Share form */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {tab === 'user' && (
              <input
                placeholder="Recipient email…"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleShare()}
                style={{ height: 36, padding: '0 12px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface-2)', color: 'var(--ink)', fontSize: 13 }}
              />
            )}
            <div style={{ display: 'flex', gap: 8 }}>
              <select
                value={permission}
                onChange={(e) => setPermission(e.target.value as 'VIEW' | 'DOWNLOAD')}
                style={{ flex: 1, height: 36, padding: '0 8px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface-2)', color: 'var(--ink)', fontSize: 13 }}
              >
                <option value="VIEW">View only</option>
                <option value="DOWNLOAD">View + Download</option>
              </select>
              <input
                type="date"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                min={new Date().toISOString().split('T')[0]}
                style={{ height: 36, padding: '0 8px', borderRadius: 8, border: '1px solid var(--border)', background: 'var(--surface-2)', color: 'var(--ink)', fontSize: 13 }}
              />
              <button className="btn btn-accent" style={{ height: 36, flexShrink: 0 }} onClick={handleShare} disabled={sharing}>
                {sharing ? '…' : tab === 'user' ? 'Share' : 'Generate link'}
              </button>
            </div>
          </div>

          {/* Existing shares */}
          {!loading && (
            <div style={{ marginTop: 20 }}>
              {tab === 'user' && userShares.length > 0 && (
                <>
                  <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 8 }}>Shared with</div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                    {userShares.map((s) => (
                      <div key={s.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '7px 10px', borderRadius: 8, background: 'var(--surface-2)' }}>
                        <Icon name="mail" size={13} style={{ color: 'var(--ink-3)', flexShrink: 0 }} />
                        <span style={{ flex: 1, fontSize: 13 }}>{s.sharedWithEmail}</span>
                        <span style={{ fontSize: 11, color: 'var(--ink-3)' }}>{s.permission}</span>
                        <button className="icon-btn" style={{ color: 'var(--danger)' }} onClick={() => handleRevoke(s.id)} title="Revoke">
                          <Icon name="close" size={13} />
                        </button>
                      </div>
                    ))}
                  </div>
                </>
              )}
              {tab === 'public' && publicShares.length > 0 && (
                <>
                  <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--ink-3)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 8 }}>Active public links</div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                    {publicShares.map((s) => (
                      <div key={s.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '7px 10px', borderRadius: 8, background: 'var(--surface-2)' }}>
                        <Icon name="link" size={13} style={{ color: 'var(--ink-3)', flexShrink: 0 }} />
                        <span style={{ flex: 1, fontSize: 12, color: 'var(--ink-2)', fontFamily: 'var(--mono)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          /public/{s.token.substring(0, 8)}…
                        </span>
                        <span style={{ fontSize: 11, color: 'var(--ink-3)' }}>{s.permission}</span>
                        <button className="icon-btn" onClick={() => copyLink(s)} title="Copy link">
                          <Icon name={copied === s.id ? 'check' : 'copy'} size={13} style={{ color: copied === s.id ? 'var(--accent)' : undefined }} />
                        </button>
                        <button className="icon-btn" style={{ color: 'var(--danger)' }} onClick={() => handleRevoke(s.id)} title="Revoke">
                          <Icon name="close" size={13} />
                        </button>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}