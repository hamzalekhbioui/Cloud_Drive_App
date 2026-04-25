import { useEffect, useState } from 'react'
import { getStarredFiles, starFile } from '../api/files'
import type { FileItem } from '../api/files'
import Icon from '../components/Icon'
import FileTile from '../components/FileTile'
import FileRow from '../components/FileRow'
import FilePreviewModal from '../components/FilePreviewModal'

export default function StarredPage() {
  const [files, setFiles] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [view, setView] = useState<'grid' | 'list'>(() => (localStorage.getItem('view') as 'grid' | 'list') || 'grid')
  const [previewFile, setPreviewFile] = useState<FileItem | null>(null)

  useEffect(() => {
    getStarredFiles()
      .then(({ data }) => setFiles(data))
      .catch(() => setError('Failed to load starred files.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleStar(id: number) {
    try {
      const { data } = await starFile(id)
      if (!data.starred) {
        setFiles((prev) => prev.filter((f) => f.id !== id))
      } else {
        setFiles((prev) => prev.map((f) => f.id === id ? data : f))
      }
    } catch { setError('Failed to update star.') }
  }

  return (
    <div className="page-inner">
      <div className="page-header">
        <div>
          <div className="eyebrow">Workspace</div>
          <h1 className="title">Starred</h1>
        </div>
        <div className="view-toggle">
          <button className={view === 'grid' ? 'active' : ''} onClick={() => setView('grid')} aria-label="Grid view"><Icon name="grid" size={15} /></button>
          <button className={view === 'list' ? 'active' : ''} onClick={() => setView('list')} aria-label="List view"><Icon name="list" size={15} /></button>
        </div>
      </div>

      {error && <div style={{ padding: 12, background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))', color: 'var(--danger)', borderRadius: 10, marginBottom: 16, fontSize: 13 }}>{error}</div>}

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: 'var(--ink-3)' }}>Loading…</div>
      ) : files.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 80, color: 'var(--ink-3)' }}>
          <div style={{ fontFamily: 'var(--serif)', fontSize: 32, marginBottom: 8 }}>No starred files.</div>
          <div>Star files from All files to find them here.</div>
        </div>
      ) : view === 'grid' ? (
        <div className="file-grid">
          {files.map((f) => (
            <FileTile key={f.id} file={f} onOpen={setPreviewFile} onStar={handleStar} />
          ))}
        </div>
      ) : (
        <div className="file-list">
          <div className="file-list-head">
            <div /><div>Name</div><div>Size</div><div>Modified</div><div>Starred</div><div />
          </div>
          {files.map((f) => (
            <FileRow key={f.id} file={f} onOpen={setPreviewFile} onStar={handleStar} />
          ))}
        </div>
      )}

      {previewFile && <FilePreviewModal file={previewFile} onClose={() => setPreviewFile(null)} />}
    </div>
  )
}
