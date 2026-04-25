import { motion } from 'framer-motion'
import type { Insight } from '../../api/analytics'

const CONFIG = {
  info:    { bg: 'color-mix(in oklab, var(--info) 12%, var(--surface))',    border: 'color-mix(in oklab, var(--info) 30%, transparent)',    dot: 'var(--info)' },
  warning: { bg: 'color-mix(in oklab, var(--warn) 12%, var(--surface))',    border: 'color-mix(in oklab, var(--warn) 30%, transparent)',    dot: 'var(--warn)' },
  success: { bg: 'color-mix(in oklab, var(--ok) 12%, var(--surface))',      border: 'color-mix(in oklab, var(--ok) 30%, transparent)',      dot: 'var(--ok)' },
  tip:     { bg: 'color-mix(in oklab, var(--accent) 12%, var(--surface))',  border: 'color-mix(in oklab, var(--accent) 30%, transparent)',  dot: 'var(--accent)' },
}

function InsightIcon({ type }: { type: Insight['type'] }) {
  if (type === 'warning') return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
    </svg>
  )
  if (type === 'success') return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
    </svg>
  )
  if (type === 'tip') return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
    </svg>
  )
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>
    </svg>
  )
}

export default function SmartInsights({ data }: { data: Insight[] }) {
  return (
    <motion.div className="an-card an-wide"
      initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: 0.4 }}
    >
      <div className="an-card-label">Smart Insights</div>
      <div className="an-insights-grid">
        {data.map((ins, i) => {
          const c = CONFIG[ins.type] ?? CONFIG.info
          return (
            <motion.div key={i} className="an-insight-card"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.45 + i * 0.07 }}
              style={{ background: c.bg, border: `1px solid ${c.border}` }}
            >
              <div className="an-insight-icon" style={{ color: c.dot }}>
                <InsightIcon type={ins.type} />
              </div>
              <div>
                <div className="an-insight-msg">{ins.message}</div>
                <div className="an-insight-detail">{ins.detail}</div>
              </div>
            </motion.div>
          )
        })}
      </div>
    </motion.div>
  )
}