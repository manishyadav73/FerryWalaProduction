import { createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { render } from '@testing-library/react'
import { LanguageProvider } from '../i18n'
import { ToastProvider } from '../context/Toast'

/** Renders with the real language + toast providers. Auth is mocked per test via vi.mock. */
export function renderApp(ui, { route = '/' } = {}) {
  return render(
    createElement(MemoryRouter, { initialEntries: [route] },
      createElement(LanguageProvider, null, createElement(ToastProvider, null, ui))),
  )
}
