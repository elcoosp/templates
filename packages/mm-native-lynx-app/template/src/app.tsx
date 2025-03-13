import * as React from 'react'
import { RouterProvider, createRouter } from '@tanstack/react-router'
import { routeTree } from './routeTree.gen'

// FIXME: this crash, tried to call in try/catch useEffect bindtap no luck, need debug logger in explorer
// Set up a Router instance
const router = createRouter({
  routeTree,
  defaultPreload: 'render',
  scrollRestoration: false,
})

// Register things for typesafety
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
const App = () => {
  return <RouterProvider router={router} />
}

export default App
