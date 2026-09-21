import { useMemo, useState } from 'react'
import Icon from '../../components/Icon'

export interface AdminTableColumn<T> {
  key: string
  header: string
  sortable?: boolean
  render?: (row: T) => React.ReactNode
  value?: (row: T) => string | number
}

interface AdminTableProps<T> {
  columns: AdminTableColumn<T>[]
  data: T[]
  rowKey: (row: T) => string
  loading?: boolean
  error?: string | null
  pageSize?: number
  filterPlaceholder?: string
}

export default function AdminTable<T>({
  columns,
  data,
  rowKey,
  loading = false,
  error = null,
  pageSize = 10,
  filterPlaceholder = 'Filter results…',
}: AdminTableProps<T>) {
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(1)
  const [sort, setSort] = useState<{ key: string; direction: 'asc' | 'desc' } | null>(null)

  const filtered = useMemo(() => {
    const query = filter.trim().toLowerCase()
    return data.filter((row) => !query || columns.some((column) => String(column.value?.(row) ?? column.render?.(row) ?? '').toLowerCase().includes(query)))
  }, [columns, data, filter])

  const sorted = useMemo(() => {
    if (!sort) return filtered
    const column = columns.find((item) => item.key === sort.key)
    if (!column?.value) return filtered
    return [...filtered].sort((a, b) => {
      const left = column.value!(a)
      const right = column.value!(b)
      const comparison = String(left).localeCompare(String(right), undefined, { numeric: true })
      return sort.direction === 'asc' ? comparison : -comparison
    })
  }, [columns, filtered, sort])

  const pageCount = Math.max(1, Math.ceil(sorted.length / pageSize))
  const currentPage = Math.min(page, pageCount)
  const rows = sorted.slice((currentPage - 1) * pageSize, currentPage * pageSize)

  function handleFilter(value: string) {
    setFilter(value)
    setPage(1)
  }

  function handleSort(column: AdminTableColumn<T>) {
    if (!column.sortable) return
    setSort((current) => current?.key === column.key
      ? { key: column.key, direction: current.direction === 'asc' ? 'desc' : 'asc' }
      : { key: column.key, direction: 'asc' })
  }

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 16 }}>
        <div className="search" style={{ maxWidth: 320 }}>
          <span className="search-icon"><Icon name="search" size={15} /></span>
          <input aria-label="Filter table" placeholder={filterPlaceholder} value={filter} onChange={(event) => handleFilter(event.target.value)} />
        </div>
        {!loading && !error && <span style={{ color: 'var(--ink-3)', fontSize: 13 }}>{filtered.length} results</span>}
      </div>
      {loading ? <div style={{ padding: 32, textAlign: 'center', color: 'var(--ink-3)' }}>Loading…</div>
        : error ? <div role="alert" style={{ padding: 32, textAlign: 'center', color: 'var(--danger)' }}>{error}</div>
          : rows.length === 0 ? <div style={{ padding: 32, textAlign: 'center', color: 'var(--ink-3)' }}>No results found.</div>
            : <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead><tr>{columns.map((column) => (
                  <th key={column.key} scope="col" style={{ textAlign: 'left', padding: '10px 12px', color: 'var(--ink-3)', fontSize: 12 }}>
                    {column.sortable ? <button type="button" onClick={() => handleSort(column)} style={{ border: 0, background: 'none', color: 'inherit', padding: 0, cursor: 'pointer' }}>{column.header} {sort?.key === column.key ? (sort.direction === 'asc' ? '↑' : '↓') : '↕'}</button> : column.header}
                  </th>
                ))}</tr></thead>
                <tbody>{rows.map((row) => <tr key={rowKey(row)}>{columns.map((column) => <td key={column.key} style={{ padding: '12px', borderTop: '1px solid var(--line)', fontSize: 13 }}>{column.render ? column.render(row) : column.value?.(row)}</td>)}</tr>)}</tbody>
              </table>
            </div>}
      {!loading && !error && sorted.length > 0 && (
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 10, marginTop: 16 }}>
          <button className="icon-btn" aria-label="Previous page" disabled={currentPage === 1} onClick={() => setPage((value) => Math.max(1, value - 1))}><Icon name="chevronLeft" size={14} /></button>
          <span style={{ fontSize: 13, color: 'var(--ink-3)' }}>Page {currentPage} of {pageCount}</span>
          <button className="icon-btn" aria-label="Next page" disabled={currentPage === pageCount} onClick={() => setPage((value) => Math.min(pageCount, value + 1))}><Icon name="chevronRight" size={14} /></button>
        </div>
      )}
    </div>
  )
}
