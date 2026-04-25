import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import type { Overview } from '../../api/analytics'
import { formatBytes } from '../../utils/files'

const R = 80
const CIRCUM = 2 * Math.PI * R

function CountUp({ to, duration = 1400 }: { to: number; duration?: number }) {
  const [val, setVal] = useState(0)
  useEffect(() => {
    const start = performance.now()
    const tick = (now: number) => {
      const p = Math.min((now - start) / duration, 1)
      setVal(Math.round(p * to))
      if (p < 1) requestAnimationFrame(tick)
    }
    requestAnimationFrame(tick)
  }, [to, duration])
  return <>{val}</>
}

export default function StorageRing({ data }: { data: Overview }) {
  const pct = Math.min(data.usagePercentage, 100)
  const [offset, setOffset] = useState(CIRCUM)

  useEffect(() => {
    const t = setTimeout(() => setOffset(CIRCUM * (1 - pct / 100)), 200)
    return () => clearTimeout(t)
  }, [pct])

  const usedGB  = data.totalStorageUsed  / 1024 ** 3
  const limitTB = data.totalStorageLimit / 1024 ** 4

  return (
    <motion.div
      className="an-card an-ring-card"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      <div className="an-card-label">Storage Overview</div>

      <div className="an-ring-wrap">
        {/* Animated SVG donut ring */}
        <div className="an-ring-svg-wrap">
          <svg width={200} height={200} viewBox="0 0 200 200">
            <defs>
              <linearGradient id="ringGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%"   stopColor="#CFFF3D" />
                <stop offset="100%" stopColor="#8DB82A" />
              </linearGradient>
            </defs>
            {/* Track */}
            <circle cx={100} cy={100} r={R} fill="none"
              stroke="var(--surface-3)" strokeWidth={16} />
            {/* Fill */}
            <circle cx={100} cy={100} r={R} fill="none"
              stroke="url(#ringGrad)" strokeWidth={16}
              strokeLinecap="round"
              strokeDasharray={CIRCUM}
              strokeDashoffset={offset}
              transform="rotate(-90 100 100)"
              style={{ transition: 'stroke-dashoffset 1.4s cubic-bezier(0.4,0,0.2,1)' }}
            />
          </svg>
          {/* Centre label */}
          <div className="an-ring-centre">
            <span className="an-ring-pct">
              <CountUp to={Math.round(pct)} /><span>%</span>
            </span>
            <span className="an-ring-sub">used</span>
          </div>
        </div>

        {/* Stats */}
        <div className="an-ring-stats">
          <div className="an-ring-stat">
            <span className="an-ring-stat-label">Used</span>
            <span className="an-ring-stat-val">{formatBytes(data.totalStorageUsed)}</span>
          </div>
          <div className="an-ring-stat">
            <span className="an-ring-stat-label">Free</span>
            <span className="an-ring-stat-val">{formatBytes(data.remainingStorage)}</span>
          </div>
          <div className="an-ring-stat">
            <span className="an-ring-stat-label">Total</span>
            <span className="an-ring-stat-val">{limitTB.toFixed(0)} TB</span>
          </div>
          <div className="an-ring-stat">
            <span className="an-ring-stat-label">Files</span>
            <span className="an-ring-stat-val">{data.totalFiles.toLocaleString()}</span>
          </div>
          {/* Usage bar */}
          <div className="an-ring-bar-wrap">
            <div className="an-ring-bar-fill"
              style={{ width: `${pct}%`, transition: 'width 1.4s cubic-bezier(0.4,0,0.2,1)' }} />
          </div>
          <div className="an-ring-bar-label">
            {usedGB.toFixed(2)} GB of {limitTB.toFixed(0)} TB
          </div>
        </div>
      </div>
    </motion.div>
  )
}