export default function SkeletonCard({ height = 200 }: { height?: number }) {
  return (
    <div className="an-card" style={{ minHeight: height }}>
      <div className="an-skel an-skel-title" />
      <div className="an-skel an-skel-body" style={{ height: height - 60 }} />
    </div>
  )
}