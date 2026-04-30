import { useEffect, useState } from 'react'
import { getSharedWithMe, type ShareItem } from '../api/shares'
import Icon from '../components/Icon'
import { formatBytes, fileKind, TYPE_COLORS } from '../utils/files'

export default function SharedPage() {
  const [items, setItems] = useState<ShareItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getSharedWithMe()
      .then(({ data }) => setItems(data))
      .catch(() => setError('Failed to load shared files.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="page-inner"><div style={{ padding: 40, textAlign: 'center', color: 'var(--ink-3)' }}>Loading…</div></div>

  return (
    <div className="page-inner">
      <div className="page-header">
        <div>
          <div className="eyebrow">Collaboration</div>
          <h1 className="display">Shared with me</h1>
        </div>
      </div>

      {error && (
        <div style={{ padding: 12, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 10, marginBottom: 20, fontSize: 13 }}>
          {error}
        </div>
      )}

      {items.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 80, color: 'var(--ink-3)' }}>
          <div style={{ fontFamily: 'var(--serif)', fontSize: 32, marginBottom: 8 }}>Nothing here yet.</div>
          <div>Files other users share with you will appear here.</div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {items.map((item) => (
            <SharedRow key={item.id} item={item} />
          ))}
        </div>
      )}
    </div>
  )
}

function SharedRow({ item }: { item: ShareItem }) {
  const kind = fileKind(item.fileName.split('.').pop() ?? '')
  const color = TYPE_COLORS[kind] ?? 'var(--ink-3)'
  const streamUrl = `/api/shares/public/${item.token}/stream`

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 14,
      padding: '12px 16px', borderRadius: 12,
      background: 'var(--surface-2)', border: '1px solid var(--border)',
    }}>
      <div style={{ width: 36, height: 36, borderRadius: 8, background: `color-mix(in oklab, ${color} 15%, var(--surface))`, display: 'grid', placeItems: 'center', flexShrink: 0 }}>
        <Icon name="files" size={16} style={{ color }} />
      </div>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontWeight: 500, fontSize: 14, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {item.fileName}
        </div>
        <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>
          Shared by <strong>{item.ownerEmail}</strong>
          {item.expiresAt && (
            <> · Expires {new Date(item.expiresAt).toLocaleDateString()}</>
          )}
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
        <span style={{ fontSize: 11, padding: '3px 8px', borderRadius: 6, background: 'var(--surface-3)', color: 'var(--ink-2)', fontWeight: 500 }}>
          {item.permission}
        </span>
        <a
          href={streamUrl}
          target="_blank"
          rel="noreferrer"
          className="btn"
          style={{ height: 32, fontSize: 12, display: 'inline-flex', alignItems: 'center', gap: 5 }}
        >
          <Icon name="eye" size={13} />
          View
        </a>
        {item.permission === 'DOWNLOAD' && (
          <a
            href={streamUrl}
            download={item.fileName}
            className="btn"
            style={{ height: 32, fontSize: 12, display: 'inline-flex', alignItems: 'center', gap: 5 }}
          >
            <Icon name="download" size={13} />
            Download
          </a>
        )}
      </div>
    </div>
  )
}