import { motion } from 'framer-motion'
import type { LargestFile } from '../../api/analytics'
import { formatBytes, formatDate, fileKind, fileExt, TYPE_COLORS } from '../../utils/files'

export default function LargestFiles({ data }: { data: LargestFile[] }) {
  if (!data.length) {
    return (
      <div className="an-card">
        <div className="an-card-label">Largest Files</div>
        <div className="an-empty">No files yet.</div>
      </div>
    )
  }

  const maxSize = data[0].size

  return (
    <motion.div className="an-card"
      initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: 0.3 }}
    >
      <div className="an-card-label">Largest Files — top {data.length}</div>
      <ul className="an-lf-list">
        {data.map((f, i) => {
          const kind  = fileKind(f.type ?? '')
          const ext   = fileExt({ originalFileName: f.name, type: f.type ?? '', id: f.id, url: '', size: f.size, createdAt: f.createdAt, starred: false, deletedAt: null })
          const color = TYPE_COLORS[kind]
          const pct   = (f.size / maxSize) * 100

          return (
            <li key={f.id} className="an-lf-row">
              <span className="an-lf-rank">#{i + 1}</span>
              <div className={`file-ico ft-${kind}`} style={{ fontSize: 9, width: 32, height: 36, flexShrink: 0 }}>{ext}</div>
              <div className="an-lf-info">
                <span className="an-lf-name" title={f.name}>{f.name}</span>
                <div className="an-lf-bar-track">
                  <div className="an-lf-bar-fill"
                    style={{ width: `${pct}%`, background: color, transition: 'width 1s ease' + (i * 0.06) + 's' }} />
                </div>
              </div>
              <div className="an-lf-right">
                <span className="an-lf-size">{formatBytes(f.size)}</span>
                <span className="an-lf-date">{f.createdAt ? formatDate(f.createdAt) : '—'}</span>
              </div>
            </li>
          )
        })}
      </ul>
    </motion.div>
  )
}