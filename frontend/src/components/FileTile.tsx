import type { FileItem } from '../api/files'
import { fileKind, fileExt, formatBytes, formatDate, TYPE_COLORS } from '../utils/files'
import Icon from './Icon'

interface Props {
  file: FileItem
  selected?: boolean
  onSelect?: (id: number, multi: boolean) => void
  onOpen: (file: FileItem) => void
  onContext?: (e: React.MouseEvent, file: FileItem) => void
}

export default function FileTile({ file, selected = false, onSelect, onOpen, onContext }: Props) {
  const kind = fileKind(file.type)
  const ext = fileExt(file)
  const color = TYPE_COLORS[kind]

  return (
    <div
      className={`file-card ${selected ? 'selected' : ''}`}
      onClick={(e) => {
        if ((e.shiftKey || e.metaKey || e.ctrlKey) && onSelect) onSelect(file.id, true)
        else onOpen(file)
      }}
      onContextMenu={(e) => onContext?.(e, file)}
      onDoubleClick={() => onOpen(file)}
    >
      <div className="thumb" style={{ background: `color-mix(in oklab, ${color} 8%, var(--surface-2))` }}>
        {kind === 'img' ? (
          <img className="thumb-image" src={file.url} alt={file.originalFileName} loading="lazy" />
        ) : (
          <div className="thumb-ext" style={{ color }}>{ext}</div>
        )}
        {onSelect && (
          <button
            type="button"
            className="thumb-check"
            onClick={(e) => { e.stopPropagation(); onSelect(file.id, true) }}
            aria-label={selected ? 'Deselect' : 'Select'}
          >
            {selected && <Icon name="check" size={12} strokeWidth={2.5} />}
          </button>
        )}
      </div>
      <div className="info">
        <div className="fname">{file.originalFileName}</div>
        <div className="fmeta">{formatBytes(file.size)} · {formatDate(file.createdAt)}</div>
      </div>
    </div>
  )
}