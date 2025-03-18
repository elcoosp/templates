import { root } from '@lynx-js/react'
import { MemoryRouter, Routes, Route } from 'react-router'
import { App } from './routes/App'
import { Home } from './routes/Home'
root.render(
  <MemoryRouter>
    <Routes>
      <Route path="/" element={<App />} />
      <Route path="/home" element={<Home />} />
    </Routes>
  </MemoryRouter>,
)

if (import.meta.webpackHot) {
  import.meta.webpackHot.accept()
}
