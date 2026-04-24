import { useEffect, useState } from 'react'
import type { FileItem } from '../api/files'
import client from '../api/client'

interface Props {
  file: FileItem
  onClose: () => void
}

export default function FilePreviewModal({ file, onClose }: Props) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-title" title={file.originalFileName}>
            {file.originalFileName}
          </span>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          <PreviewContent file={file} />
        </div>
      </div>
    </div>
  )
}

function useBlobUrl(fileId: number) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let url: string | null = null
    client
      .get(`/files/${fileId}/stream`, { responseType: 'blob' })
      .then((res) => {
        url = URL.createObjectURL(res.data as Blob)
        setBlobUrl(url)
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
    return () => { if (url) URL.revokeObjectURL(url) }
  }, [fileId])

  return { blobUrl, loading, error }
}

function PreviewContent({ file }: { file: FileItem }) {
  const { blobUrl, loading, error } = useBlobUrl(file.id)
  const { type, originalFileName } = file

  if (loading) {
    return <div className="preview-loading">Loading preview…</div>
  }

  if (error || !blobUrl) {
    return (
      <div className="preview-unavailable">
        <span style={{ fontSize: 48 }}>⚠️</span>
        <p>Could not load preview.</p>
        <a href={file.url} target="_blank" rel="noreferrer" className="btn-primary preview-download">
          Download instead
        </a>
      </div>
    )
  }

  if (type?.startsWith('image/')) {
    return <img src={blobUrl} alt={originalFileName} className="preview-image" />
  }

  if (type === 'application/pdf') {
    return <iframe src={blobUrl} title={originalFileName} className="preview-iframe" />
  }

  if (type?.startsWith('video/')) {
    return <video controls src={blobUrl} className="preview-video" />
  }

  if (type?.startsWith('audio/')) {
    return (
      <div className="preview-audio">
        <span className="preview-audio-icon">🎵</span>
        <p>{originalFileName}</p>
        <audio controls src={blobUrl} />
      </div>
    )
  }

  if (type?.startsWith('text/')) {
    return <TextPreview blobUrl={blobUrl} />
  }

  return (
    <div className="preview-unavailable">
      <span style={{ fontSize: 48 }}>📁</span>
      <p>Preview not available for this file type.</p>
      <a href={file.url} target="_blank" rel="noreferrer" className="btn-primary preview-download">
        Download file
      </a>
    </div>
  )
}

function TextPreview({ blobUrl }: { blobUrl: string }) {
  const [text, setText] = useState<string | null>(null)

  useEffect(() => {
    fetch(blobUrl).then((r) => r.text()).then(setText)
  }, [blobUrl])

  if (text === null) return <p style={{ color: '#94A3B8' }}>Loading…</p>
  return <pre className="preview-text">{text}</pre>
}