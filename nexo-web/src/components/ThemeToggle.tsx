import { useEffect, useState } from 'react'

type Theme = 'system' | 'light' | 'dark'

const NEXT: Record<Theme, Theme> = { system: 'light', light: 'dark', dark: 'system' }
const LABEL: Record<Theme, string> = { system: 'Tema do sistema', light: 'Tema claro', dark: 'Tema escuro' }
const ICON: Record<Theme, string> = { system: 'A', light: '☀', dark: '☾' }

/**
 * Tema segue o sistema (`prefers-color-scheme`), com troca manual guardada neste navegador.
 */
export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(() => read())

  useEffect(() => {
    if (theme === 'system') {
      document.documentElement.removeAttribute('data-theme')
    } else {
      document.documentElement.setAttribute('data-theme', theme)
    }

    try {
      localStorage.setItem('nexo.theme', theme)
    } catch {
      // sem armazenamento local: o tema vale só nesta aba
    }
  }, [theme])

  return (
    <button type="button" className="icon-button" aria-label={LABEL[theme]} title={LABEL[theme]} onClick={() => setTheme(NEXT[theme])}>
      {ICON[theme]}
    </button>
  )
}

function read(): Theme {
  try {
    const saved = localStorage.getItem('nexo.theme')

    return saved === 'light' || saved === 'dark' ? saved : 'system'
  } catch {
    return 'system'
  }
}
