import { useEffect, useRef, useState } from 'react'
import { deleteFile, getMyFiles, uploadFile } from '../api/files'
import type { FileItem } from '../api/files'
import Icon from '../components/Icon'
import FileTile from '../components/FileTile'
import FileRow from '../components/FileRow'
import FilePreviewModal from '../components/FilePreviewModal'
import { fileKind, typeLabel } from '../utils/files'

const TYPE_FILTERS = ['all', 'pdf', 'doc', 'sheet', 'deck', 'img', 'video', 'zip'] as const

export default function FilesPage() {
  const [files, setFiles] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [view, setView] = useState<'grid' | 'list'>(() => (localStorage.getItem('view') as 'grid' | 'list') || 'grid')
  const [filter, setFilter] = useState<typeof TYPE_FILTERS[number]>('all')
  const [selected, setSelected] = useState<number[]>([])
  const [previewFile, setPreviewFile] = useState<FileItem | null>(null)
  const [error, setError] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => { fetchFiles() }, [])
  useEffect(() => { localStorage.setItem('view', view) }, [view])

  async function fetchFiles() {
    try { const { data } = await getMyFiles(); setFiles(data) }
    catch { setError('Failed to load files.') }
    finally { setLoading(false) }
  }

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true); setError('')
    try {
      const { data } = await uploadFile(file)
      setFiles((prev) => [data, ...prev])
    } catch { setError('Upload failed.') }
    finally { setUploading(false); if (inputRef.current) inputRef.current.value = '' }
  }

  async function handleDelete(ids: number[]) {
    if (!confirm(`Delete ${ids.length} file${ids.length > 1 ? 's' : ''}?`)) return
    try {
      await Promise.all(ids.map((id) => deleteFile(id)))
      setFiles((prev) => prev.filter((f) => !ids.includes(f.id)))
      setSelected([])
    } catch { setError('Failed to delete.') }
  }

  const toggleSelect = (id: number, _multi: boolean) =>
    setSelected((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))

  const filtered = filter === 'all' ? files : files.filter((f) => fileKind(f.type) === filter)

  return (
    <div className="page-inner">
      <div className="page-header">
        <div>
          <div className="eyebrow">Workspace</div>
          <h1 className="title">All files</h1>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <div className="view-toggle">
            <button className={view === 'grid' ? 'active' : ''} onClick={() => setView('grid')} aria-label="Grid view"><Icon name="grid" size={15} /></button>
            <button className={view === 'list' ? 'active' : ''} onClick={() => setView('list')} aria-label="List view"><Icon name="list" size={15} /></button>
          </div>
          <button className="btn btn-accent" onClick={() => inputRef.current?.click()} disabled={uploading}>
            <Icon name="upload" size={14} /> {uploading ? 'Uploading…' : 'Upload'}
          </button>
          <input ref={inputRef} type="file" hidden onChange={handleUpload} />
        </div>
      </div>

      <div className="browser-toolbar">
        <div className="filters">
          {TYPE_FILTERS.map((t) => (
            <button
              key={t}
              className={`chip ${filter === t ? 'active' : ''}`}
              onClick={() => setFilter(t)}
            >
              {t === 'all' ? 'All types' : typeLabel(t as ReturnType<typeof fileKind>) + 's'}
            </button>
          ))}
        </div>
      </div>

      {error && <div style={{ padding: 12, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 10, marginBottom: 16, fontSize: 13 }}>{error}</div>}

      {selected.length > 0 && (
        <div className="batch-bar">
          <span className="count">{selected.length} selected</span>
          <span className="divider" />
          <button className="bbtn" onClick={() => handleDelete(selected)}><Icon name="trash" size={14} /> Delete</button>
          <button className="bbtn close" onClick={() => setSelected([])}><Icon name="close" size={14} /></button>
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--ink-3)' }}>Loading…</div>
      ) : filtered.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 80, color: 'var(--ink-3)' }}>
          <div style={{ fontFamily: 'var(--serif)', fontSize: 32, marginBottom: 8 }}>Nothing here yet.</div>
          <div>Upload your first file to get started.</div>
        </div>
      ) : view === 'grid' ? (
        <div className="file-grid">
          {filtered.map((f) => (
            <FileTile
              key={f.id}
              file={f}
              selected={selected.includes(f.id)}
              onSelect={toggleSelect}
              onOpen={setPreviewFile}
            />
          ))}
        </div>
      ) : (
        <div className="file-list">
          <div className="file-list-head">
            <div /><div>Name</div><div>Size</div><div>Modified</div><div>Owner</div><div />
          </div>
          {filtered.map((f) => (
            <FileRow
              key={f.id}
              file={f}
              selected={selected.includes(f.id)}
              onSelect={toggleSelect}
              onOpen={setPreviewFile}
            />
          ))}
        </div>
      )}

      {previewFile && (
        <FilePreviewModal
          file={previewFile}
          onClose={() => setPreviewFile(null)}
          onDelete={async () => { await handleDelete([previewFile.id]); setPreviewFile(null) }}
        />
      )}
    </div>
  )
}