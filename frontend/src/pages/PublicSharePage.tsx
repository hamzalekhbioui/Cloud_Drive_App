import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import client from '../api/client'
import Icon from '../components/Icon'
import { formatSize } from '../utils/files'

interface PublicFile {
  id: number
  originalFileName: string
  size: number
  type: string
  createdAt: string
  url: string
}

export default function PublicSharePage() {
  const { token } = useParams<{ token: string }>()
  const [file, setFile] = useState<PublicFile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) return
    client.get<PublicFile>(`/public/${token}`)
      .then(({ data }) => setFile(data))
      .catch((err) => {
        const msg = err.response?.data?.message || 'Link is invalid, expired, or revoked.'
        setError(msg)
      })
      .finally(() => setLoading(false))
  }, [token])

  if (loading) return <div style={{ display: 'grid', placeItems: 'center', height: '100vh', color: 'var(--ink-3)' }}>Loading shared file…</div>

  if (error) return (
    <div style={{ display: 'grid', placeItems: 'center', height: '100vh', padding: 20, textAlign: 'center' }}>
      <div style={{ maxWidth: 400 }}>
        <Icon name="close" size={48} style={{ color: 'var(--danger)', marginBottom: 20 }} />
        <h1 style={{ fontFamily: 'var(--serif)', fontSize: 32, marginBottom: 12 }}>Unable to access file</h1>
        <p style={{ color: 'var(--ink-3)', lineHeight: 1.6 }}>{error}</p>
        <a href="/" style={{ display: 'inline-block', marginTop: 24, padding: '10px 20px', background: 'var(--ink)', color: 'var(--bg)', borderRadius: 8, fontWeight: 600 }}>Go to Vault</a>
      </div>
    </div>
  )

  const handleDownload = () => {
    window.location.href = `/public/${token}/stream?download=true`
  }

  const isPreviewable = file?.type?.startsWith('image/') || file?.type === 'application/pdf'

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)', display: 'flex', flexDirection: 'column' }}>
      <header style={{ padding: '20px 28px', borderBottom: '1px solid var(--line)', display: 'flex', alignItems: 'center', gap: 12, background: 'var(--surface)' }}>
        <img src="/cloude_logo.jpeg" alt="Vault" width="28" height="28" style={{ objectFit: 'contain' }} />
        <span style={{ fontWeight: 700, fontSize: 18, letterSpacing: '-0.02em' }}>Vault</span>
        <span style={{ color: 'var(--ink-4)', fontSize: 14 }}>/ Shared file</span>
      </header>

      <main style={{ flex: 1, display: 'grid', placeItems: 'center', padding: 40 }}>
        <div style={{ width: '100%', maxWidth: 640, background: 'var(--surface)', borderRadius: 24, border: '1px solid var(--line)', padding: 40, textAlign: 'center', boxShadow: 'var(--shadow-lg)' }}>
          <div style={{ width: 80, height: 80, borderRadius: 20, background: 'var(--surface-2)', display: 'grid', placeItems: 'center', margin: '0 auto 24px' }}>
            <Icon name="folder" size={40} style={{ color: 'var(--ink-2)' }} />
          </div>

          <h1 style={{ fontFamily: 'var(--serif)', fontSize: 36, marginBottom: 8, wordBreak: 'break-all' }}>{file?.originalFileName}</h1>
          <div style={{ color: 'var(--ink-3)', fontSize: 14, marginBottom: 32 }}>
            {file && formatSize(file.size)} • Shared on {file && new Date(file.createdAt).toLocaleDateString()}
          </div>

          <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
            {isPreviewable && (
              <a href={`/public/${token}/stream`} target="_blank" rel="noreferrer" className="btn btn-secondary" style={{ height: 48, padding: '0 24px', fontSize: 15 }}>
                View file
              </a>
            )}
            <button onClick={handleDownload} className="btn btn-accent" style={{ height: 48, padding: '0 24px', fontSize: 15 }}>
              <Icon name="download" size={18} /> Download
            </button>
          </div>

          <div style={{ marginTop: 40, paddingTop: 32, borderTop: '1px solid var(--line)', fontSize: 12, color: 'var(--ink-4)' }}>
            Securely shared via Vault Cloud Drive. Links may expire or be revoked by the owner.
          </div>
        </div>
      </main>

      <footer style={{ padding: '24px 28px', textAlign: 'center', fontSize: 12, color: 'var(--ink-4)', fontFamily: 'var(--mono)', borderTop: '1px solid var(--line)' }}>
        Built by Hamza
      </footer>
    </div>
  )
}
