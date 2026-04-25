import { motion } from 'framer-motion'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer,
} from 'recharts'
import type { ActivityItem } from '../../api/analytics'
import { formatBytes } from '../../utils/files'

function fmt(date: string) {
  const d = new Date(date + 'T00:00:00')
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

function CustomTooltip({ active, payload, label }: {
  active?: boolean; payload?: { value: number }[]; label?: string
}) {
  if (!active || !payload?.length) return null
  return (
    <div className="an-tooltip">
      <div className="an-tooltip-label">{label ? fmt(label) : ''}</div>
      <div className="an-tooltip-val">{formatBytes(payload[0].value)}</div>
      <div className="an-tooltip-pct">{payload[1]?.value ?? 0} file{payload[1]?.value === 1 ? '' : 's'}</div>
    </div>
  )
}

function tickFmt(val: number) {
  if (val === 0) return '0'
  if (val < 1024 * 1024) return `${(val / 1024).toFixed(0)}K`
  return `${(val / (1024 * 1024)).toFixed(0)}M`
}

function labelFmt(date: string) {
  const d = new Date(date + 'T00:00:00')
  // Show only every 5th label to avoid clutter
  if (d.getDate() % 5 !== 0) return ''
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

export default function ActivityChart({ data }: { data: ActivityItem[] }) {
  return (
    <motion.div className="an-card an-wide"
      initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: 0.2 }}
    >
      <div className="an-card-label">Upload Activity — last 30 days</div>
      <ResponsiveContainer width="100%" height={200}>
        <AreaChart data={data} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="actGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%"  stopColor="#CFFF3D" stopOpacity={0.25} />
              <stop offset="95%" stopColor="#CFFF3D" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--line)" vertical={false} />
          <XAxis dataKey="date" tickFormatter={labelFmt}
            tick={{ fontSize: 11, fill: 'var(--ink-4)', fontFamily: 'var(--mono)' }}
            axisLine={false} tickLine={false} />
          <YAxis tickFormatter={tickFmt}
            tick={{ fontSize: 11, fill: 'var(--ink-4)', fontFamily: 'var(--mono)' }}
            axisLine={false} tickLine={false} width={40} />
          <Tooltip content={<CustomTooltip />} />
          <Area type="monotone" dataKey="totalUploadedSize"
            stroke="#CFFF3D" strokeWidth={2}
            fill="url(#actGrad)" dot={false} activeDot={{ r: 4, fill: '#CFFF3D' }} />
          {/* hidden area for fileCount so tooltip can read it */}
          <Area type="monotone" dataKey="fileCount"
            stroke="transparent" fill="transparent" dot={false} />
        </AreaChart>
      </ResponsiveContainer>
    </motion.div>
  )
}