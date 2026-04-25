import { useEffect, useState } from 'react'
import type { FileItem } from '../api/files'
import { fileKind, fileExt, formatBytes, formatDate, typeLabel, TYPE_COLORS } from '../utils/files'
import Icon from './Icon'
import client from '../api/client'

interface Props {
  file: FileItem
  onClose: () => void
  onDelete?: () => void
}

function usePdfBlobUrl(fileId: number, enabled: boolean) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)

  useEffect(() => {
    if (!enabled) return
    let url: string | null = null
    setLoading(true)
    client
      .get(`/files/${fileId}/stream`, { responseType: 'blob' })
      .then((res) => {
        url = URL.createObjectURL(new Blob([res.data as BlobPart], { type: 'application/pdf' }))
        setBlobUrl(url)
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
    return () => { if (url) URL.revokeObjectURL(url) }
  }, [fileId, enabled])

  return { blobUrl, loading, error }
}

export default function FilePreviewModal({ file, onClose, onDelete }: Props) {
  const kind = fileKind(file.type)
  const ext = fileExt(file)
  const color = TYPE_COLORS[kind]

  const { blobUrl: pdfBlobUrl, loading: pdfLoading, error: pdfError } = usePdfBlobUrl(file.id, kind === 'pdf')

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => { document.removeEventListener('keydown', onKey); document.body.style.overflow = '' }
  }, [onClose])

  function renderCanvas() {
    if (kind === 'img') {
      return <img src={file.url} alt={file.originalFileName} style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain', borderRadius: 8 }} />
    }
    if (kind === 'video') {
      return <video src={file.url} controls style={{ maxWidth: '100%', maxHeight: '100%', borderRadius: 8 }} />
    }
    if (kind === 'audio') {
      return <audio src={file.url} controls />
    }
    if (kind === 'pdf') {
      if (pdfLoading) {
        return <div style={{ color: 'var(--ink-3)', fontFamily: 'var(--mono)', fontSize: 12 }}>Loading PDF…</div>
      }
      if (pdfError || !pdfBlobUrl) {
        return (
          <div style={{ textAlign: 'center', color: 'var(--ink-3)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <div style={{ fontFamily: 'var(--serif)', fontSize: 48, opacity: 0.4 }}>PDF</div>
            <div style={{ fontSize: 13 }}>Preview unavailable.</div>
            <a className="btn btn-secondary btn-sm" href={file.url} target="_blank" rel="noreferrer">
              <Icon name="download" size={13} /> Download to view
            </a>
          </div>
        )
      }
      return <iframe src={pdfBlobUrl} title={file.originalFileName} style={{ width: '100%', height: '100%', border: 0, borderRadius: 8, background: '#fff' }} />
    }
    return <div style={{ fontFamily: 'var(--serif)', fontSize: 96, color, opacity: 0.4 }}>{ext}</div>
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal preview-modal" onClick={(e) => e.stopPropagation()}>
        <div className="preview-canvas" style={{ background: `color-mix(in oklab, ${color} 4%, var(--surface-2))` }}>
          {renderCanvas()}
          <button
            type="button"
            className="icon-btn"
            onClick={onClose}
            style={{ position: 'absolute', top: 12, right: 12, background: 'var(--surface)', color: 'var(--ink)', boxShadow: 'var(--shadow-sm)' }}
            aria-label="Close"
          >
            <Icon name="close" size={16} />
          </button>
        </div>
        <div className="preview-side">
          <div>
            <div className="eyebrow" style={{ color }}>{typeLabel(kind)} · {ext}</div>
            <div className="fname" style={{ marginTop: 4 }}>{file.originalFileName}</div>
          </div>
          <div className="meta-list">
            <div className="meta-row"><span className="k">Size</span><span className="v">{formatBytes(file.size)}</span></div>
            <div className="meta-row"><span className="k">Uploaded</span><span className="v">{formatDate(file.createdAt)}</span></div>
            <div className="meta-row"><span className="k">Type</span><span className="v">{file.type || 'unknown'}</span></div>
          </div>
          <div className="side-actions">
            <a className="btn btn-secondary" href={file.url} target="_blank" rel="noreferrer">
              <Icon name="download" size={14} /> Download
            </a>
            {onDelete && (
              <button type="button" className="btn btn-danger" onClick={onDelete}>
                <Icon name="trash" size={14} /> Delete
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
