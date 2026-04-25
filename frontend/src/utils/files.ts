import type { FileItem } from '../api/files'

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  if (bytes < 1024 ** 3) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024 ** 3).toFixed(2)} GB`
}

export function formatDate(iso: string): string {
  const dt = new Date(iso)
  const diffH = (Date.now() - dt.getTime()) / 36e5
  if (diffH < 1) return `${Math.max(1, Math.round(diffH * 60))}m ago`
  if (diffH < 24) return `${Math.round(diffH)}h ago`
  const diffD = diffH / 24
  if (diffD < 7) return `${Math.round(diffD)}d ago`
  return dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

export type FileKind = 'pdf' | 'doc' | 'sheet' | 'deck' | 'img' | 'video' | 'zip' | 'audio' | 'other'

export function fileKind(mime: string): FileKind {
  if (mime.startsWith('image/')) return 'img'
  if (mime.startsWith('video/')) return 'video'
  if (mime.startsWith('audio/')) return 'audio'
  if (mime.includes('pdf')) return 'pdf'
  if (mime.includes('zip') || mime.includes('compressed') || mime.includes('rar') || mime.includes('tar')) return 'zip'
  if (mime.includes('word') || mime.includes('document') || mime.includes('text')) return 'doc'
  if (mime.includes('sheet') || mime.includes('excel') || mime.includes('csv')) return 'sheet'
  if (mime.includes('presentation') || mime.includes('powerpoint') || mime.includes('keynote')) return 'deck'
  return 'other'
}

export function fileExt(file: FileItem): string {
  const m = file.originalFileName.match(/\.([^.]+)$/)
  if (m) return m[1].slice(0, 4).toUpperCase()
  return ({ pdf: 'PDF', doc: 'DOC', sheet: 'XLS', deck: 'KEY', img: 'IMG', video: 'MP4', zip: 'ZIP', audio: 'MP3', other: 'FILE' } as const)[fileKind(file.type)]
}

export function typeLabel(kind: FileKind): string {
  return ({ pdf: 'Document', doc: 'Document', sheet: 'Spreadsheet', deck: 'Presentation', img: 'Image', video: 'Video', zip: 'Archive', audio: 'Audio', other: 'File' } as const)[kind]
}

export const TYPE_COLORS: Record<FileKind, string> = {
  pdf: '#C23A1A', doc: '#2B4FCC', sheet: '#1E7A3C', deck: '#B5671A',
  img: '#6B2BB8', video: '#146E6B', zip: '#5A5A5E', audio: '#B2185A', other: '#5A5A5E',
}

export const TOTAL_STORAGE_BYTES = 1024 * 1024 * 1024 * 1024 // 1 TB