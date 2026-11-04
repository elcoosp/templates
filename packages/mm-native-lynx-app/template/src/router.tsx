import {
  Children,
  createContext,
  createElement,
  useContext,
  useMemo,
  useState,
} from '@lynx-js/react'

type RouterContextValue = {
  path: string
  navigate: (nextPath: string) => void
}

const RouterContext = createContext<RouterContextValue | null>(null)

type RouteProps = {
  path: string
  element: unknown
}

type RouteElementLike = {
  props?: RouteProps
}

function MemoryRouter(props: { children?: unknown }) {
  const [path, setPath] = useState('/')
  const value = useMemo<RouterContextValue>(
    () => ({
      path,
      navigate: (nextPath: string) => {
        setPath(nextPath)
      },
    }),
    [path],
  )
  return createElement(RouterContext.Provider, { value }, props.children)
}

function Routes(props: { children?: unknown }) {
  const ctx = useContext(RouterContext)
  if (ctx === null) {
    return null
  }
  let matched: RouteProps | undefined
  Children.forEach(props.children, (child) => {
    if (matched !== undefined) {
      return
    }
    const route = child as RouteElementLike
    if (route?.props?.path === ctx.path) {
      matched = route.props
    }
  })
  if (matched === undefined) {
    return null
  }
  return matched.element
}

function Route(_props: RouteProps) {
  // Routes reads the route table from its children, so a single Route
  // never renders on its own.
  return null
}

function useNavigate() {
  const ctx = useContext(RouterContext)
  return useMemo(
    () => (nextPath: string) => {
      ctx?.navigate(nextPath)
    },
    [ctx],
  )
}

function useLocation() {
  const ctx = useContext(RouterContext)
  return useMemo(() => ({ pathname: ctx?.path ?? '/' }), [ctx])
}

export { MemoryRouter, Route, Routes, useLocation, useNavigate }
