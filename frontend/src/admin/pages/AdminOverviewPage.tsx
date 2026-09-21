import { useEffect, useMemo, useState } from 'react'
import { motion } from 'framer-motion'
import { getAdminGrowth, getAdminOverview, getAdminStorage } from '../api/overview'
import type { AdminGrowthPoint, AdminOverview, AdminStorageOverview } from '../api/overview'
import ActivityChart from '../../components/analytics/ActivityChart'
import BreakdownChart from '../../components/analytics/BreakdownChart'
import type { ActivityItem, BreakdownItem } from '../../api/analytics'
import { formatBytes } from '../../utils/files'

const COLORS = ['#CFFF3D', '#6B2BB8', '#146E6B', '#2B4FCC', '#B2185A', '#5A5A5E']

function Kpi({ label, value, detail }: { label: string; value: string; detail?: string }) {
  return (
    <motion.div className="card" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
      <div className="eyebrow">{label}</div>
      <div style={{ fontSize: 28, fontWeight: 700, marginTop: 8 }}>{value}</div>
      {detail && <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 5 }}>{detail}</div>}
    </motion.div>
  )
}

function AdminGrowthChart({ data }: { data: AdminGrowthPoint[] }) {
  const activity: ActivityItem[] = data.map((point) => ({
    date: point.date,
    totalUploadedSize: point.uploadedBytes,
    fileCount: point.uploads,
    signups: point.signups,
  }))
  return <ActivityChart data={activity} showSignups />
}

export default function AdminOverviewPage() {
  const [overview, setOverview] = useState<AdminOverview | null>(null)
  const [growth, setGrowth] = useState<AdminGrowthPoint[] | null>(null)
  const [storage, setStorage] = useState<AdminStorageOverview | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([getAdminOverview(), getAdminGrowth(), getAdminStorage()])
      .then(([overviewResponse, growthResponse, storageResponse]) => {
        setOverview(overviewResponse.data)
        setGrowth(growthResponse.data)
        setStorage(storageResponse.data)
      })
      .catch(() => setError('Failed to load the admin overview.'))
  }, [])

  const breakdown = useMemo<BreakdownItem[]>(() => {
    if (!storage) return []
    const total = storage.byFileType.reduce((sum, item) => sum + item.bytes, 0)
    return storage.byFileType.map((item, index) => ({
      category: item.category,
      size: item.bytes,
      percentage: total > 0 ? (item.bytes * 100) / total : 0,
      color: COLORS[index % COLORS.length],
    }))
  }, [storage])

  return (
    <div className="page-inner">
      <div className="page-header">
        <div><div className="eyebrow">Administration</div><h1 className="title">Platform overview</h1></div>
      </div>
      {error && <div role="alert" style={{ color: 'var(--danger)', marginBottom: 16 }}>{error}</div>}
      {!overview || !growth || !storage ? (
        <div className="an-grid"><div className="card">Loading overview…</div></div>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: 12, marginBottom: 16 }}>
            <Kpi label="Total users" value={overview.totalUsers.toLocaleString()} detail={`${overview.activeUsers.toLocaleString()} active`} />
            <Kpi label="Files" value={overview.totalFiles.toLocaleString()} detail={`${overview.trashedFiles.toLocaleString()} trashed`} />
            <Kpi label="Storage used" value={formatBytes(overview.bytesStored)} />
            <Kpi label="Active shares" value={overview.activeShares.toLocaleString()} detail={`${overview.publicShares} public · ${overview.privateShares} private`} />
            <Kpi label="Teams" value={overview.teams.toLocaleString()} />
            <Kpi label="MRR" value={`$${(overview.mrrCents / 100).toFixed(2)}`} detail={`${overview.activeSubscriptions.reduce((sum, plan) => sum + plan.count, 0)} active subscriptions`} />
          </div>
          <div className="an-grid">
            <AdminGrowthChart data={growth} />
            <BreakdownChart data={breakdown} />
          </div>
          <div className="card" style={{ marginTop: 16 }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 20 }}>
              <div>
                <div className="an-card-label">Storage by plan</div>
                {storage.byPlan.length === 0 ? <div style={{ color: 'var(--ink-3)', marginTop: 12 }}>No subscription storage yet.</div> : storage.byPlan.map((item) => (
                  <div key={item.category} style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10, fontSize: 13 }}>
                    <span>{item.category}</span><strong>{formatBytes(item.bytes)}</strong>
                  </div>
                ))}
              </div>
              <div>
            <div className="an-card-label">AI processing status</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 20, marginTop: 14 }}>
              {overview.aiProcessing.length === 0 ? <span style={{ color: 'var(--ink-3)' }}>No AI processing jobs.</span> : overview.aiProcessing.map((item) => (
                <div key={item.status}><strong>{item.count.toLocaleString()}</strong> <span style={{ color: 'var(--ink-3)' }}>{item.status.toLowerCase()}</span></div>
              ))}
            </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
