import { useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'

export interface ToastState {
  message: string
  type: 'success' | 'error'
}

interface Props {
  toast: ToastState | null
  onDismiss: () => void
}

export default function Toast({ toast, onDismiss }: Props) {
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(onDismiss, 3200)
    return () => clearTimeout(t)
  }, [toast, onDismiss])

  return (
    <AnimatePresence>
      {toast && (
        <motion.div
          key={toast.message + toast.type}
          className={`sett-toast sett-toast-${toast.type}`}
          initial={{ opacity: 0, y: -12, scale: 0.96 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: -12, scale: 0.96 }}
          transition={{ duration: 0.2 }}
        >
          <span className="sett-toast-dot" />
          {toast.message}
        </motion.div>
      )}
    </AnimatePresence>
  )
}