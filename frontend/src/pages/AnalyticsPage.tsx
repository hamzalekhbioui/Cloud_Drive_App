import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import {
  getOverview, getBreakdown, getLargestFiles, getActivity, getInsights,
} from '../api/analytics'
import type { Overview, BreakdownItem, LargestFile, ActivityItem, Insight } from '../api/analytics'
import StorageRing    from '../components/analytics/StorageRing'
import BreakdownChart from '../components/analytics/BreakdownChart'
import ActivityChart  from '../components/analytics/ActivityChart'
import LargestFiles   from '../components/analytics/LargestFiles'
import SmartInsights  from '../components/analytics/SmartInsights'
import SkeletonCard   from '../components/analytics/SkeletonCard'

export default function AnalyticsPage() {
  const [overview,      setOverview]      = useState<Overview | null>(null)
  const [breakdown,     setBreakdown]     = useState<BreakdownItem[] | null>(null)
  const [largestFiles,  setLargestFiles]  = useState<LargestFile[] | null>(null)
  const [activity,      setActivity]      = useState<ActivityItem[] | null>(null)
  const [insights,      setInsights]      = useState<Insight[] | null>(null)
  const [error,         setError]         = useState('')

  useEffect(() => {
    // Fetch all five endpoints in parallel
    Promise.all([
      getOverview(),
      getBreakdown(),
      getLargestFiles(),
      getActivity(),
      getInsights(),
    ]).then(([ov, bd, lf, ac, ins]) => {
      setOverview(ov.data)
      setBreakdown(bd.data)
      setLargestFiles(lf.data)
      setActivity(ac.data)
      setInsights(ins.data)
    }).catch((err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg || 'Failed to load analytics. Make sure the backend is running.')
    })
  }, [])

  const loading = !overview || !breakdown || !largestFiles || !activity || !insights

  return (
    <div className="page-inner">
      <motion.div className="page-header"
        initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        <div>
          <div className="eyebrow">Workspace</div>
          <h1 className="title">Storage Analytics</h1>
        </div>
      </motion.div>

      {error && (
        <div style={{
          padding: 12, marginBottom: 16, fontSize: 13, borderRadius: 10,
          background: 'color-mix(in oklab, var(--danger) 10%, var(--surface))',
          color: 'var(--danger)',
        }}>{error}</div>
      )}

      {loading && !error ? (
        <div className="an-grid">
          <SkeletonCard height={280} />
          <SkeletonCard height={280} />
          <SkeletonCard height={240} />
          <SkeletonCard height={360} />
          <SkeletonCard height={240} />
        </div>
      ) : !error && overview && breakdown && largestFiles && activity && insights ? (
        <div className="an-grid">
          {/* Row 1: ring + breakdown */}
          <StorageRing     data={overview} />
          <BreakdownChart  data={breakdown} />

          {/* Row 2: activity (full width) */}
          <ActivityChart   data={activity} />

          {/* Row 3: largest files + insights */}
          <LargestFiles    data={largestFiles} />
          <SmartInsights   data={insights} />
        </div>
      ) : null}
    </div>
  )
}