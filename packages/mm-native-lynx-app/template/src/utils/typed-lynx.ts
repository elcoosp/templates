import { NODE_REF_INVOKE_ERROR_CODE, type SelectorQuery } from '@lynx-js/types'

// Define UI element types with direct method signatures
export interface UIElementBase {
  tag: string // The JSX tag name
  methods: Record<string, (...args: any[]) => any>
}

// Map UI element types to their selectors (for type inference)
export interface UIElementMap {
  // Empty by default, will be extended via declaration merging
}

// Type helper for getting element type from selector
export type ElementTypeFromSelector<T extends string> = T extends `#${infer ID}`
  ? ID extends keyof UIElementMap
    ? UIElementMap[ID]
    : UIElementBase
  : UIElementBase

// Get methods available for an element type
export type MethodsOf<T extends UIElementBase> = keyof T['methods']

// Extract parameter types from a method function signature
export type ParamsOf<
  TElement extends UIElementBase,
  TMethod extends MethodsOf<TElement>
> = TElement['methods'][TMethod] extends (params: infer P) => any
  ? P
  : never

// Extract return type from a method function signature
export type ReturnTypeOf<
  TElement extends UIElementBase,
  TMethod extends MethodsOf<TElement>
> = TElement['methods'][TMethod] extends (...args: any[]) => infer R
  ? R
  : never

// Generic type for invoke options with improved type inference
export type TypedInvokeOptions<
  TElement extends UIElementBase,
  TMethod extends MethodsOf<TElement>,
> = {
  method: TMethod
  params: ParamsOf<TElement, TMethod>
  success?(res: ReturnTypeOf<TElement, TMethod>): void
  fail?(res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }): void
}

// Enhance the NodesRef interface via declaration merging
declare module '@lynx-js/types' {
  interface NodesRef {
    invoke<TElement extends UIElementBase, TMethod extends MethodsOf<TElement>>(
      options: TypedInvokeOptions<TElement, TMethod>,
    ): SelectorQuery
  }

  // Add uiMethodOptions type
  interface uiMethodOptions {
    fail?(res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }): void
  }
}

// Generic typed selector query function with improved type inference
export function invokeExec<
  TSelector extends string,
  TElement extends UIElementBase = ElementTypeFromSelector<TSelector>,
  TMethod extends MethodsOf<TElement> = MethodsOf<TElement>,
>(
  selector: TSelector,
  method: TMethod,
  params: ParamsOf<TElement, TMethod>,
  success: (res: ReturnTypeOf<TElement, TMethod>) => void,
  fail: (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => void,
): void {
  return lynx
    .createSelectorQuery()
    .select(selector)
    .invoke({
      method,
      params,
      success,
      fail,
    })
    .exec()
}

// Higher-level typed helper functions
export function createTypedInvoker<TSelector extends keyof UIElementMap>(
  selector: TSelector,
) {
  type Element = UIElementMap[TSelector]

  return {
    invoke: <TMethod extends MethodsOf<Element>>(
      method: TMethod,
      params: ParamsOf<Element, TMethod>,
      success: (res: ReturnTypeOf<Element, TMethod>) => void,
      fail?: (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => void,
    ) => {
      invokeExec(
        `#${String(selector)}`,
        method,
        params,
        success,
        fail ||
          ((error) =>
            console.error(`Error invoking ${String(method)}:`, error)),
      )
    },
  }
}

// Generic element invoker with direct type extraction
export function invokeElementMethod<
  TSelector extends string,
  TElement extends UIElementBase = ElementTypeFromSelector<TSelector>,
  TMethod extends MethodsOf<TElement> = MethodsOf<TElement>,
>(
  selector: TSelector,
  method: TMethod,
  params: ParamsOf<TElement, TMethod> = {} as ParamsOf<TElement, TMethod>,
  success?: (res: ReturnTypeOf<TElement, TMethod>) => void,
  fail?: { (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }): void },
): void {
  invokeExec(
    selector,
    method,
    params,
    (res) => {
      success?.(res)
    },
    (res) => {
      fail?.(res)
      console.error(
        'lynx',
        `${String(method)} method failed`,
        JSON.stringify(res),
      )
    },
  )
}

// Extract all available tags from UIElementMap
type AvailableTags = UIElementMap[keyof UIElementMap]['tag']

// Create tag-based invokers with proper constraint
export function createTagInvoker<TTag extends AvailableTags>(
  tag: TTag,
  id: string,
) {
  // Find the element type that matches this tag
  type MatchingElements = {
    [K in keyof UIElementMap]: UIElementMap[K]['tag'] extends typeof tag
      ? UIElementMap[K]
      : never
  }

  type ElementType = MatchingElements[keyof MatchingElements]

  return {
    invoke: <TMethod extends MethodsOf<ElementType>>(
      method: TMethod,
      params?: ParamsOf<ElementType, TMethod>,
      success?: (res: ReturnTypeOf<ElementType, TMethod>) => void,
      fail?: { (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }): void },
    ) => {
      invokeElementMethod(
        `#${id}`,
        method,
        params || ({} as ParamsOf<ElementType, TMethod>),
        success,
        fail,
      )
    },
  }
}