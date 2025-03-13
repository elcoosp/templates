import { root } from '@lynx-js/react'
import App from './app'
root.render(<App />)

if (import.meta.webpackHot) {
  import.meta.webpackHot.accept()
}
