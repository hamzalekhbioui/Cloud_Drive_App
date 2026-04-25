import { useEffect, useState } from 'react'
import { getTrashFiles, restoreFile, permanentlyDeleteFile } from '../api/files'
import type { FileItem } from '../api/files'
import Icon from '../components/Icon'
import { fileKind, fileExt, formatBytes, formatDate } from '../utils/files'

function InfoIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" />
    </svg>
  )
}

export default function TrashPage() {
  const [files, setFiles] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getTrashFiles()
      .then(({ data }) => setFiles(data))
      .catch(() => setError('Failed to load trash.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleRestore(id: number) {
    try {
      await restoreFile(id)
      setFiles((prev) => prev.filter((f) => f.id !== id))
    } catch { setError('Failed to restore file.') }
  }

  async function handlePermanentDelete(id: number) {
    if (!confirm('Permanently delete this file? This cannot be undone.')) return
    try {
      await permanentlyDeleteFile(id)
      setFiles((prev) => prev.filter((f) => f.id !== id))
    } catch { setError('Failed to delete file.') }
  }

  async function handleEmptyTrash() {
    if (!confirm(`Permanently delete all ${files.length} files in trash? This cannot be undone.`)) return
    try {
      await Promise.all(files.map((f) => permanentlyDeleteFile(f.id)))
      setFiles([])
    } catch { setError('Failed to empty trash.') }
  }

  return (
    <div className="page-inner">
      <div className="page-header">
        <div>
          <div className="eyebrow">Workspace</div>
          <h1 className="title">Trash</h1>
        </div>
        {files.length > 0 && (
          <button
            className="btn"
            style={{ background: 'color-mix(in oklab, var(--danger) 12%, var(--surface))', color: 'var(--danger)', border: '1px solid color-mix(in oklab, var(--danger) 30%, transparent)' }}
            onClick={handleEmptyTrash}
          >
            <Icon name="trash" size={14} /> Empty trash
          </button>
        )}
      </div>

      {files.length > 0 && (
        <div style={{ padding: '10px 16px', background: 'color-mix(in oklab, var(--warn) 10%, var(--surface))', color: 'var(--warn)', borderRadius: 10, marginBottom: 20, fontSize: 13, display: 'flex', alignItems: 'center', gap: 8 }}>
          <InfoIcon />
          Files in trash are not accessible and will be permanently deleted after 30 days.
        </div>
      )}

      {error && <div style={{ padding: 12, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 10, marginBottom: 16, fontSize: 13 }}>{error}</div>}

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--ink-3)' }}>Loading…</div>
      ) : files.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 80, color: 'var(--ink-3)' }}>
          <div style={{ fontFamily: 'var(--serif)', fontSize: 32, marginBottom: 8 }}>Trash is empty.</div>
          <div>Deleted files will appear here.</div>
        </div>
      ) : (
        <div className="file-list">
          <div className="file-list-head">
            <div /><div>Name</div><div>Size</div><div>Deleted</div><div /><div />
          </div>
          {files.map((f) => {
            const kind = fileKind(f.type)
            const ext = fileExt(f)
            return (
              <div key={f.id} className="file-list-row">
                <div />
                <div className="fcell-name">
                  <div className={`file-ico ft-${kind}`} style={{ opacity: 0.5 }}>{ext}</div>
                  <div style={{ minWidth: 0 }}>
                    <div className="fname" style={{ opacity: 0.6 }}>{f.originalFileName}</div>
                  </div>
                </div>
                <div className="fcell-meta">{formatBytes(f.size)}</div>
                <div className="fcell-meta">{f.deletedAt ? formatDate(f.deletedAt) : '—'}</div>
                <div className="fcell-meta">
                  <button
                    type="button"
                    className="btn"
                    style={{ fontSize: 12, padding: '4px 10px', height: 'auto' }}
                    onClick={() => handleRestore(f.id)}
                  >
                    Restore
                  </button>
                </div>
                <button
                  type="button"
                  className="icon-btn"
                  style={{ width: 28, height: 28, color: 'var(--danger)' }}
                  onClick={() => handlePermanentDelete(f.id)}
                  aria-label="Delete permanently"
                >
                  <Icon name="trash" size={14} />
                </button>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
