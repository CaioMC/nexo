import { useEffect } from 'react'

export interface ToastMessage {
  tone: 'success' | 'warning' | 'danger'
  text: string
}

export function Toast({ toast, onClose }: { toast?: ToastMessage; onClose: () => void }) {
  useEffect(() => {
    if (!toast) {
      return
    }

    const timer = setTimeout(onClose, 6000)

    return () => clearTimeout(timer)
  }, [toast, onClose])

  if (!toast) {
    return null
  }

  return (
    <div className={`toast toast-${toast.tone}`} role="status">
      {toast.text}
      <button type="button" className="icon-button" aria-label="Fechar" onClick={onClose}>
        ×
      </button>
    </div>
  )
}
