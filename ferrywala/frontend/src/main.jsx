import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@fontsource-variable/fraunces'
import '@fontsource-variable/plus-jakarta-sans'
import '@fontsource-variable/noto-sans-devanagari'
import '@fontsource-variable/noto-sans-tamil'
import './styles/global.css'
import './styles/pages.css'
import './styles/concierge.css'
import './styles/motion.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
