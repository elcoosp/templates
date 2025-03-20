import { NODE_REF_INVOKE_ERROR_CODE, type SelectorQuery } from '@lynx-js/types'

// Define the Result type to handle success and failure
export type InvocationResult<TData = void, TError = { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }> = 
  | { ok: true; data: TData }
  | { ok: false; error: TError }

// Base UI element interface without tag (removed redundancy)
export interface UIElementBase {
  methods: Record<string, (...args: any[]) => any>
  allowedSelectors?: string[] // Used for type checking only
}

// Map UI element types to their definitions (for type inference)
export interface UIElementMap {
  // Empty by default, will be extended via declaration merging
}

// Type helper for getting element type from selector
export type ElementTypeFromSelector<T extends string> = 
  T extends `#${infer ID}`
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
> = Parameters<TElement['methods'][TMethod]>[0] extends undefined
  ? {}
  : Parameters<TElement['methods'][TMethod]>[0]

// Extract return type from a method function signature
export type ReturnTypeOf<
  TElement extends UIElementBase,
  TMethod extends MethodsOf<TElement>
> = ReturnType<TElement['methods'][TMethod]>

/**
 * Type utility that extracts the allowed selectors from a UI element type.
 * If the element has allowedSelectors, it returns those selectors.
 * Otherwise, it returns string (allowing any selector).
 */
export type AllowedSelectorsOf<TTag extends keyof UIElementMap> = 
  UIElementMap[TTag] extends { allowedSelectors: infer TSelectors }
    ? TSelectors extends string[]
      ? TSelectors[number]
      : string
    : string

/**
 * Helper type that gets an allowed ID from a UI element type.
 * Strips the '#' prefix from allowed selectors if present.
 */
export type AllowedIdOf<TTag extends keyof UIElementMap> =
  AllowedSelectorsOf<TTag> extends `#${infer TId}`
    ? TId
    : AllowedSelectorsOf<TTag>

/**
 * Type guard to ensure an ID is allowed for a specific element type.
 * Returns the ID if it's allowed, otherwise returns never.
 */
export type EnsureIdAllowed<
  TTag extends keyof UIElementMap,
  TId extends string
> = TId extends AllowedIdOf<TTag> ? TId : never

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
  callback?: (result: InvocationResult<ReturnTypeOf<TElement, TMethod>, { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }>) => void,
): void {
  return lynx
    .createSelectorQuery()
    .select(selector)
    .invoke({
      method,
      params,
      success: (res: ReturnTypeOf<TElement, TMethod>) => {
        callback?.({ ok: true, data: res });
      },
      fail: (error: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => {
        callback?.({ ok: false, error });
      },
    })
    .exec()
}

// Type guard to ensure selector is allowed at compile time
type EnsureSelectorAllowed<
  TElement extends UIElementBase,
  TSelector extends string
> = TElement['allowedSelectors'] extends string[]
  ? TSelector extends TElement['allowedSelectors'][number] 
    ? TSelector 
    : never
  : TSelector;

// Higher-level typed helper functions with compile-time selector validation
export function createTypedInvoker<
  TTag extends keyof UIElementMap,
  TId extends string
>(
  tag: TTag,
  id: TId
) {
  type TElement = UIElementMap[TTag];
  // This type constraint runs at compile time
  type ValidSelector = EnsureSelectorAllowed<TElement, `#${TId}`>;
  
  // This will cause a compile error if the selector is not allowed
  // because ValidSelector will be 'never' in that case
  return {
    invoke: <TMethod extends MethodsOf<TElement>>(
      method: TMethod,
      params?: ParamsOf<TElement, TMethod>,
      callback?: (result: InvocationResult<ReturnTypeOf<TElement, TMethod>, { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }>) => void,
    ) => {
      invokeExec<ValidSelector, TElement, TMethod>(
        `#${id}` as ValidSelector, // Cast is safe because of the type constraint
        method,
        params || ({} as ParamsOf<TElement, TMethod>),
        callback
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
  callback?: (result: InvocationResult<ReturnTypeOf<TElement, TMethod>, { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }>) => void,
): void {
  invokeExec<TSelector, TElement, TMethod>(
    selector,
    method,
    params,
    callback
  )
}