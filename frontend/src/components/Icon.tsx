import type { CSSProperties } from 'react'

type IconName =
  | 'home' | 'folder' | 'files' | 'share' | 'users' | 'star' | 'trash' | 'clock'
  | 'chart' | 'search' | 'plus' | 'upload' | 'download' | 'more' | 'grid' | 'list'
  | 'lock' | 'sun' | 'moon' | 'bell' | 'settings' | 'chevronRight' | 'chevronLeft'
  | 'chevronDown' | 'close' | 'check' | 'link' | 'copy' | 'edit' | 'move' | 'eye'
  | 'filter' | 'layers' | 'menu' | 'restore' | 'mail' | 'shield' | 'key' | 'trend' | 'tag'

interface IconProps {
  name: IconName
  size?: number
  className?: string
  strokeWidth?: number
  style?: CSSProperties
}

export default function Icon({ name, size = 18, className, strokeWidth = 1.6, style }: IconProps) {
  const common = {
    width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
    stroke: 'currentColor', strokeWidth,
    strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const,
    className, style,
  }
  const paths: Record<IconName, JSX.Element> = {
    home: <><path d="M3 10.5 12 3l9 7.5" /><path d="M5 9.5V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.5" /></>,
    folder: <><path d="M3 6a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6Z" /></>,
    files: <><path d="M9 3h6l5 5v11a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" /><path d="M14 3v5h5" /></>,
    share: <><circle cx="6" cy="12" r="2.5" /><circle cx="18" cy="6" r="2.5" /><circle cx="18" cy="18" r="2.5" /><path d="m8 11 8-4" /><path d="m8 13 8 4" /></>,
    users: <><circle cx="9" cy="9" r="3.5" /><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6" /><circle cx="17" cy="8" r="2.5" /><path d="M21 19c0-2.5-1.8-4.5-4.2-4.9" /></>,
    star: <><path d="m12 3.5 2.6 5.3 5.9.9-4.3 4.2 1 5.8L12 17l-5.2 2.7 1-5.8L3.5 9.7l5.9-.9L12 3.5Z" /></>,
    trash: <><path d="M4 7h16" /><path d="M10 11v6M14 11v6" /><path d="M6 7h12l-1 13a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L6 7Z" /><path d="M9 7V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v3" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    chart: <><path d="M4 20V10" /><path d="M10 20V4" /><path d="M16 20v-7" /><path d="M22 20H2" /></>,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    upload: <><path d="M12 3v13" /><path d="m7 8 5-5 5 5" /><path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" /></>,
    download: <><path d="M12 3v13" /><path d="m7 12 5 5 5-5" /><path d="M4 19v1a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-1" /></>,
    more: <><circle cx="5" cy="12" r="1.2" /><circle cx="12" cy="12" r="1.2" /><circle cx="19" cy="12" r="1.2" /></>,
    grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
    list: <><path d="M4 6h16M4 12h16M4 18h16" /></>,
    lock: <><rect x="5" y="11" width="14" height="9" rx="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" /></>,
    sun: <><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></>,
    moon: <><path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z" /></>,
    bell: <><path d="M6 9a6 6 0 1 1 12 0v5l1.5 2H4.5L6 14V9Z" /><path d="M10 20a2 2 0 0 0 4 0" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.6 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.6-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3h.1a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8v.1a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" /></>,
    chevronRight: <><path d="m9 6 6 6-6 6" /></>,
    chevronLeft: <><path d="m15 6-6 6 6 6" /></>,
    chevronDown: <><path d="m6 9 6 6 6-6" /></>,
    close: <><path d="M6 6l12 12M18 6 6 18" /></>,
    check: <><path d="m5 12 5 5 9-11" /></>,
    link: <><path d="M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1" /><path d="M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1" /></>,
    copy: <><rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" /></>,
    edit: <><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 1 1 3 3L7 19l-4 1 1-4 12.5-12.5Z" /></>,
    move: <><path d="M12 3v18M3 12h18" /><path d="m8 7-4 5 4 5M16 7l4 5-4 5" /></>,
    eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></>,
    filter: <><path d="M3 5h18l-7 8v5l-4 2v-7L3 5Z" /></>,
    layers: <><path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 13 9 5 9-5" /></>,
    menu: <><path d="M3 6h18M3 12h18M3 18h18" /></>,
    restore: <><path d="M3 12a9 9 0 1 0 3-6.7" /><path d="M3 4v5h5" /></>,
    mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
    shield: <><path d="m12 3 8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3Z" /></>,
    key: <><circle cx="8" cy="16" r="4" /><path d="m10 14 8-8 2 2-2 2 2 2-2 2-2-2-2 2" /></>,
    trend: <><path d="M3 17 9 11l4 4 8-8" /><path d="M17 7h4v4" /></>,
    tag: <><path d="M3 12V4a1 1 0 0 1 1-1h8l8 8-9 9-8-8Z" /><circle cx="8" cy="8" r="1.5" /></>,
  }
  return <svg {...common}>{paths[name]}</svg>
}