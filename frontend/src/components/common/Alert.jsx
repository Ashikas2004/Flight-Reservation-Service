import React from 'react'
import { AlertCircle, CheckCircle, XCircle, Info } from 'lucide-react'
import { clsx } from 'clsx'

const variants = {
  success: {
    icon: CheckCircle,
    classes: 'bg-emerald-50 text-emerald-800 border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-200 dark:border-emerald-800',
  },
  error: {
    icon: XCircle,
    classes: 'bg-rose-50 text-rose-800 border-rose-200 dark:bg-rose-950/60 dark:text-rose-200 dark:border-rose-800',
  },
  warning: {
    icon: AlertCircle,
    classes: 'bg-amber-50 text-amber-800 border-amber-200 dark:bg-amber-950/60 dark:text-amber-200 dark:border-amber-800',
  },
  info: {
    icon: Info,
    classes: 'bg-blue-50 text-blue-800 border-blue-200 dark:bg-blue-950/60 dark:text-blue-200 dark:border-blue-800',
  },
}

export default function Alert({ type = 'info', message, onClose, className }) {
  const { icon: Icon, classes } = variants[type] ?? variants.info
  if (!message) return null

  return (
    <div className={clsx('flex items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-xs transition-colors', classes, className)}>
      <Icon className="w-5 h-5 mt-0.5 flex-shrink-0" />
      <p className="flex-1 font-medium">{message}</p>
      {onClose && (
        <button onClick={onClose} className="text-current opacity-60 hover:opacity-100 transition-opacity" aria-label="Close">
          <XCircle className="w-4 h-4" />
        </button>
      )}
    </div>
  )
}
