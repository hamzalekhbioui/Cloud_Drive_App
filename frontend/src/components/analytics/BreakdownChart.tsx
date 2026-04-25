import { useState } from 'react'
import { motion } from 'framer-motion'
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts'
import type { BreakdownItem } from '../../api/analytics'
import { formatBytes } from '../../utils/files'

function CustomTooltip({ active, payload }: { active?: boolean; payload?: { payload: BreakdownItem }[] }) {
  if (!active || !payload?.length) return null
  const d = payload[0].payload
  return (
    <div className="an-tooltip">
      <div className="an-tooltip-label">{d.category}</div>
      <div className="an-tooltip-val">{formatBytes(d.size)}</div>
      <div className="an-tooltip-pct">{d.percentage.toFixed(1)}%</div>
    </div>
  )
}


export default function BreakdownChart({ data }: { data: BreakdownItem[] }) {
  const [active, setActive] = useState<number | undefined>()

  if (!data.length) {
    return (
      <div className="an-card">
        <div className="an-card-label">Storage Breakdown</div>
        <div className="an-empty">No files yet.</div>
      </div>
    )
  }

  return (
    <motion.div className="an-card"
      initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: 0.1 }}
    >
      <div className="an-card-label">Storage Breakdown</div>
      <div className="an-breakdown-wrap">
        <ResponsiveContainer width="100%" height={200}>
          <PieChart>
            <Pie data={data} cx="50%" cy="50%" innerRadius={60} outerRadius={90}
              dataKey="size" paddingAngle={2}
              onMouseEnter={(_, i) => setActive(i)}
              onMouseLeave={() => setActive(undefined)}
            >
              {data.map((entry) => (
                <Cell key={entry.category} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
          </PieChart>
        </ResponsiveContainer>

        {/* Legend */}
        <ul className="an-legend">
          {data.map((item, i) => (
            <li key={item.category}
              className={`an-legend-item${active === i ? ' active' : ''}`}
              onMouseEnter={() => setActive(i)}
              onMouseLeave={() => setActive(undefined)}
            >
              <span className="an-legend-dot" style={{ background: item.color }} />
              <span className="an-legend-name">{item.category}</span>
              <span className="an-legend-size">{formatBytes(item.size)}</span>
              <span className="an-legend-pct">{item.percentage.toFixed(0)}%</span>
            </li>
          ))}
        </ul>
      </div>
    </motion.div>
  )
}