import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyFiles, uploadFile, deleteFile } from '../api/files'
import type { FileItem } from '../api/files'
import { useAuth } from '../context/AuthContext'
import FilePreviewModal from '../components/FilePreviewModal'

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

function fileIcon(type: string): string {
  if (type.startsWith('image/')) return '🖼️'
  if (type.startsWith('video/')) return '🎬'
  if (type.startsWith('audio/')) return '🎵'
  if (type.includes('pdf')) return '📄'
  if (type.includes('zip') || type.includes('compressed')) return '🗜️'
  if (type.includes('word') || type.includes('document')) return '📝'
  if (type.includes('sheet') || type.includes('excel')) return '📊'
  return '📁'
}

export default function DashboardPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [files, setFiles] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [previewFile, setPreviewFile] = useState<FileItem | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    fetchFiles()
  }, [])

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

  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    setError('')
    try {
      const { data } = await uploadFile(file)
      setFiles((prev) => [data, ...prev])
    } catch {
      setError('Upload failed. Please try again.')
    } finally {
      setUploading(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  async function handleDelete(fileId: number) {
    if (!confirm('Delete this file?')) return
    try {
      await deleteFile(fileId)
      setFiles((prev) => prev.filter((f) => f.id !== fileId))
    } catch {
      setError('Failed to delete file.')
    }
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="dashboard">
      <header className="navbar">
        <div className="navbar-brand">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
            <path d="M3 15C3 17.8 5.2 20 8 20H18C20.2 20 22 18.2 22 16C22 14.1 20.7 12.5 18.9 12.1C18.6 9.2 16.1 7 13 7C10.6 7 8.5 8.3 7.4 10.3C4.9 10.7 3 12.7 3 15Z" fill="#2563EB"/>
          </svg>
          <span>CloudDrive</span>
        </div>
        <div className="navbar-user">
          <span className="avatar">{user?.name.charAt(0).toUpperCase()}</span>
          <span className="user-name">{user?.name}</span>
          <button className="btn-logout" onClick={handleLogout}>Sign out</button>
        </div>
      </header>

      <main className="main">
        <div className="main-header">
          <h2>My Files</h2>
          <button
            className="btn-primary btn-upload"
            onClick={() => inputRef.current?.click()}
            disabled={uploading}
          >
            {uploading ? 'Uploading…' : '+ Upload file'}
          </button>
          <input ref={inputRef} type="file" hidden onChange={handleUpload} />
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {loading ? (
          <div className="empty-state">Loading…</div>
        ) : files.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">☁️</div>
            <p>No files yet. Upload your first file!</p>
          </div>
        ) : (
          <div className="file-grid">
            {files.map((file) => (
              <div key={file.id} className="file-card">
                <div className="file-icon">{fileIcon(file.type)}</div>
                <div className="file-info">
                  <p className="file-name" title={file.originalFileName}>
                    {file.originalFileName}
                  </p>
                  <p className="file-meta">{formatBytes(file.size)} · {formatDate(file.createdAt)}</p>
                </div>
                <div className="file-actions">
                  <button className="btn-icon" onClick={() => setPreviewFile(file)} title="Open">
                    👁
                  </button>
                  <a href={file.url} target="_blank" rel="noreferrer" className="btn-icon" title="Download">
                    ⬇
                  </a>
                  <button className="btn-icon btn-danger" onClick={() => handleDelete(file.id)} title="Delete">
                    🗑
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
      {previewFile && (
        <FilePreviewModal file={previewFile} onClose={() => setPreviewFile(null)} />
      )}
    </div>
  )
}