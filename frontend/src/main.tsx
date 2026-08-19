import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from './app/providers/AuthProvider'
import { AppRouter } from './app/router/AppRouter'
import './styles.css'
createRoot(document.getElementById('root')!).render(<StrictMode><AuthProvider><AppRouter/></AuthProvider></StrictMode>)

