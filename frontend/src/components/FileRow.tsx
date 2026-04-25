import type { FileItem } from '../api/files'
import { fileKind, fileExt, formatBytes, formatDate } from '../utils/files'
import Icon from './Icon'

interface Props {
  file: FileItem
  selected?: boolean
  onSelect?: (id: number, multi: boolean) => void
  onOpen: (file: FileItem) => void
  onContext?: (e: React.MouseEvent, file: FileItem) => void
}

export default function FileRow({ file, selected = false, onSelect, onOpen, onContext }: Props) {
  const kind = fileKind(file.type)
  const ext = fileExt(file)

  return (
    <div
      className={`file-list-row ${selected ? 'selected' : ''}`}
      onClick={(e) => {
        if ((e.shiftKey || e.metaKey || e.ctrlKey) && onSelect) onSelect(file.id, true)
        else onOpen(file)
      }}
      onContextMenu={(e) => onContext?.(e, file)}
      onDoubleClick={() => onOpen(file)}
    >
      <button
        type="button"
        className="checkbox"
        onClick={(e) => { e.stopPropagation(); onSelect?.(file.id, true) }}
        aria-label={selected ? 'Deselect' : 'Select'}
      >
        {selected && <Icon name="check" size={11} strokeWidth={2.5} />}
      </button>
      <div className="fcell-name">
        <div className={`file-ico ft-${kind}`}>{ext}</div>
        <div style={{ minWidth: 0 }}>
          <div className="fname">{file.originalFileName}</div>
        </div>
      </div>
      <div className="fcell-meta">{formatBytes(file.size)}</div>
      <div className="fcell-meta">{formatDate(file.createdAt)}</div>
      <div className="fcell-meta">You</div>
      <button
        type="button"
        className="icon-btn"
        style={{ width: 28, height: 28 }}
        onClick={(e) => { e.stopPropagation(); onContext?.(e, file) }}
        aria-label="More actions"
      >
        <Icon name="more" size={15} />
      </button>
    </div>
  )
}