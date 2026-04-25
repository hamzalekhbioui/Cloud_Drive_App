import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyFiles, starFile, uploadFile } from '../api/files'
import type { FileItem } from '../api/files'
import { useAuth } from '../context/AuthContext'
import Icon from '../components/Icon'
import FileTile from '../components/FileTile'
import FilePreviewModal from '../components/FilePreviewModal'
import {
  formatBytes, fileKind, typeLabel, TYPE_COLORS, TOTAL_STORAGE_BYTES,
} from '../utils/files'

export default function DashboardPage() {
  const { user } = useAuth()
  const [files, setFiles] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [previewFile, setPreviewFile] = useState<FileItem | null>(null)
  const [dragging, setDragging] = useState(false)
  const dragCounter = useRef(0)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => { fetchFiles() }, [])

  async function fetchFiles() {
    try {
      const { data } = await getMyFiles()
      setFiles(data)
    } catch {
      setError('Failed to load files.')
    } finally {
      setLoading(false)
    }
  }

  async function doUpload(file: File) {
    setUploading(true); setError('')
    try {
      const { data } = await uploadFile(file)
      setFiles((prev) => [data, ...prev])
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Upload failed. Check that the backend is running.')
    } finally {
      setUploading(false)
    }
  }

  async function handleStar(id: number) {
    try {
      const { data } = await starFile(id)
      setFiles((prev) => prev.map((f) => f.id === id ? data : f))
    } catch { setError('Failed to update star.') }
  }

  useEffect(() => {
    const onEnter = (e: DragEvent) => { e.preventDefault(); dragCounter.current++; if (e.dataTransfer?.types?.includes('Files')) setDragging(true) }
    const onLeave = (e: DragEvent) => { e.preventDefault(); dragCounter.current--; if (dragCounter.current <= 0) setDragging(false) }
    const onOver = (e: DragEvent) => e.preventDefault()
    const onDrop = (e: DragEvent) => {
      e.preventDefault(); dragCounter.current = 0; setDragging(false)
      const dropped = Array.from(e.dataTransfer?.files || [])
      dropped.forEach(doUpload)
    }
    window.addEventListener('dragenter', onEnter)
    window.addEventListener('dragleave', onLeave)
    window.addEventListener('dragover', onOver)
    window.addEventListener('drop', onDrop)
    return () => {
      window.removeEventListener('dragenter', onEnter)
      window.removeEventListener('dragleave', onLeave)
      window.removeEventListener('dragover', onOver)
      window.removeEventListener('drop', onDrop)
    }
  }, [])

  const used = files.reduce((s, f) => s + f.size, 0)
  const pct = (used / TOTAL_STORAGE_BYTES) * 100
  const recents = [...files].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 6)

  const byKind: Record<string, number> = {}
  files.forEach((f) => { const k = fileKind(f.type); byKind[k] = (byKind[k] || 0) + f.size })
  const breakdown = Object.entries(byKind).map(([k, v]) => ({ kind: k as ReturnType<typeof fileKind>, size: v })).sort((a, b) => b.size - a.size).slice(0, 6)
  const breakdownTotal = breakdown.reduce((s, b) => s + b.size, 0) || 1

  const greeting = (() => {
    const h = new Date().getHours()
    if (h < 12) return 'Good morning'
    if (h < 18) return 'Good afternoon'
    return 'Good evening'
  })()

  return (
    <div className="page-inner">
      <div className="page-header">
        <div>
          <div className="eyebrow">{new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}</div>
          <h1 className="display">{greeting}, <em style={{ fontStyle: 'italic', color: 'var(--ink-3)' }}>{user?.name.split(' ')[0]}</em>.</h1>
        </div>
        <button type="button" className="btn btn-accent" onClick={() => inputRef.current?.click()} disabled={uploading}>
          <Icon name="upload" size={15} /> {uploading ? 'Uploading…' : 'Upload files'}
        </button>
        <input
          ref={inputRef}
          type="file"
          style={{ display: 'none' }}
          onChange={(e) => { const f = e.target.files?.[0]; if (f) void doUpload(f); if (inputRef.current) inputRef.current.value = '' }}
        />
      </div>

      {error && <div style={{ padding: 12, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 10, marginBottom: 20, fontSize: 13 }}>{error}</div>}

      <div className="hero-grid">
        <div className="storage-hero">
          <div className="accent-ring" />
          <div style={{ position: 'relative' }}>
            <div className="eyebrow">Storage in use</div>
            <div className="hero-num">
              {(used / 1024 ** 3).toFixed(2)}<span className="unit">GB</span>
            </div>
            <div className="hero-of">of 1,024 GB on Business plan</div>
          </div>
          <div style={{ position: 'relative' }}>
            <div className="hero-bar"><div className="fill" style={{ width: `${pct}%` }} /></div>
            <div className="hero-foot">
              <div className="label">{pct.toFixed(2)}% utilized · {files.length} files total</div>
            </div>
          </div>
        </div>

        <div className="breakdown-card">
          <h3>Breakdown<span className="eyebrow">by type</span></h3>
          <div className="breakdown-list">
            {breakdown.length === 0 ? (
              <div style={{ color: 'var(--ink-3)', fontSize: 13 }}>Upload files to see a breakdown.</div>
            ) : breakdown.map((b) => (
              <div className="breakdown-row" key={b.kind}>
                <div className="swatch" style={{ background: TYPE_COLORS[b.kind] }} />
                <div className="name">{typeLabel(b.kind)}s</div>
                <div className="bar-wrap"><div className="bar" style={{ width: `${(b.size / breakdownTotal) * 100}%`, background: TYPE_COLORS[b.kind] }} /></div>
                <div className="size">{formatBytes(b.size)}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="section-head">
        <h2 className="h2">Recently added</h2>
        <Link to="/files" style={{ fontSize: 12, color: 'var(--ink-3)', fontWeight: 500 }}>View all →</Link>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--ink-3)' }}>Loading…</div>
      ) : recents.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 80, color: 'var(--ink-3)' }}>
          <div style={{ fontFamily: 'var(--serif)', fontSize: 32, marginBottom: 8 }}>Your vault is empty.</div>
          <div>Drag files anywhere on this page to upload — they'll encrypt automatically.</div>
        </div>
      ) : (
        <div className="file-grid">
          {recents.map((f) => (
            <FileTile key={f.id} file={f} onOpen={setPreviewFile} onStar={handleStar} />
          ))}
        </div>
      )}

      {previewFile && <FilePreviewModal file={previewFile} onClose={() => setPreviewFile(null)} />}

      <div className={`dragdrop-overlay ${dragging ? 'active' : ''}`}>
        <div className="dragdrop-card">
          <div className="t">Drop to upload</div>
          <div className="s">Files encrypt automatically on upload</div>
        </div>
      </div>
    </div>
  )
}
