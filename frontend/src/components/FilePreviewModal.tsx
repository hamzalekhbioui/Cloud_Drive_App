import { useEffect, useState } from 'react'
import { chatWithFile, getAiStatus, retryAi, type AiCitation, type FileItem } from '../api/files'
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
    // eslint-disable-next-line react-hooks/set-state-in-effect
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
  const aiSupported = kind === 'pdf' || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const [aiStatus, setAiStatus] = useState<import('../api/files').AiStatus | null>(file.aiStatus ? {
    status: file.aiStatus, error: file.aiError ?? null, summary: file.aiSummary ?? null, processedAt: null
  } : null)
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState<{ text: string; citations: AiCitation[] } | null>(null)
  const [chatLoading, setChatLoading] = useState(false)
  const [chatError, setChatError] = useState('')
  const [aiStatusError, setAiStatusError] = useState('')

  useEffect(() => {
    if (!aiSupported) return
    let active = true
    const load = () => getAiStatus(file.id)
      .then(({ data }) => {
        if (!active) return
        setAiStatusError('')
        setAiStatus(data)
      })
      .catch(() => {
        if (active) setAiStatusError('Unable to check AI processing status.')
      })
    void load()
    const timer = window.setInterval(() => {
      if (aiStatus?.status === 'COMPLETED' || aiStatus?.status === 'ERROR' || aiStatus?.status === 'UNSUPPORTED') return
      void load()
    }, 2500)
    return () => { active = false; window.clearInterval(timer) }
  }, [file.id, aiSupported, aiStatus?.status])

  async function askFile() {
    if (!question.trim()) return
    setChatLoading(true); setChatError('')
    try {
      const { data } = await chatWithFile(file.id, question.trim())
      setAnswer({ text: data.answer, citations: data.citations })
      setQuestion('')
    } catch (err: unknown) {
      const error = err as { response?: { status?: number; data?: { message?: string } } }
      setChatError(error.response?.status === 402
        ? 'You have reached your monthly AI query limit. Upgrade your plan to continue.'
        : error.response?.data?.message ?? 'Unable to answer this question.')
    } finally { setChatLoading(false) }
  }

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
          {aiSupported && (
            <div style={{ marginTop: 22, borderTop: '1px solid var(--border)', paddingTop: 18 }}>
              <div className="eyebrow" style={{ color: 'var(--accent)' }}>AI file chat</div>
              {aiStatusError ? (
                <div style={{ color: 'var(--danger)', fontSize: 13, marginTop: 8 }}>{aiStatusError}</div>
              ) : !aiStatus || aiStatus.status === 'PENDING' || aiStatus.status === 'PROCESSING' ? (
                <div style={{ color: 'var(--ink-3)', fontSize: 13, marginTop: 8 }}>Preparing this file for questions…</div>
              ) : aiStatus.status === 'UNSUPPORTED' || aiStatus.status === 'ERROR' ? (
                <div style={{ color: 'var(--danger)', fontSize: 13, marginTop: 8 }}>
                  {aiStatus.error || 'AI processing failed.'}
                  <button type="button" className="btn btn-secondary btn-sm" style={{ marginTop: 8 }} onClick={() => void retryAi(file.id).then(({ data }) => setAiStatus(data))}>Retry</button>
                </div>
              ) : (
                <>
                  {aiStatus.summary && <div style={{ fontSize: 12, color: 'var(--ink-3)', margin: '8px 0 12px', lineHeight: 1.45 }}>{aiStatus.summary}</div>}
                  <div style={{ display: 'flex', gap: 6 }}>
                    <input value={question} onChange={(e) => setQuestion(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') void askFile() }} placeholder="Ask this file…" maxLength={2000} style={{ minWidth: 0, flex: 1 }} />
                    <button type="button" className="btn btn-accent btn-sm" onClick={() => void askFile()} disabled={chatLoading || !question.trim()}>{chatLoading ? '…' : 'Ask'}</button>
                  </div>
                  {chatError && <div style={{ color: 'var(--danger)', fontSize: 12, marginTop: 8 }}>{chatError}</div>}
                  {answer && <div style={{ marginTop: 12, fontSize: 13, lineHeight: 1.5 }}>
                    <div>{answer.text}</div>
                    {answer.citations.length > 0 && (
                      <div style={{ marginTop: 8, color: 'var(--ink-3)', fontSize: 11 }}>
                        {answer.citations.map((c) => <div key={c.chunkIndex}>{c.source ?? `Source ${c.chunkIndex}`}: {c.excerpt}</div>)}
                      </div>
                    )}
                  </div>}
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
