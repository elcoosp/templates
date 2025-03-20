import { NODE_REF_INVOKE_ERROR_CODE, type SelectorQuery } from '@lynx-js/types'

// Define the Result type to handle success and failure
export type InvocationResult<
  TData = void,
  TError = { code: NODE_REF_INVOKE_ERROR_CODE; data?: any },
> = { ok: true; data: TData } | { ok: false; error: TError }

// Base UI element interface
export interface UIElementBase {
  methods: Record<string, (...args: any[]) => any>
}

// Map UI element types to their definitions (for type inference)
export interface UIElementMap {
  // Empty by default, will be extended via declaration merging
}

// Define a custom error type for more descriptive compile-time errors
type ErrorMessage<T extends string> = {
  __errorMessage: T
  __validSelector: never
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
  TMethod extends MethodsOf<TElement>,
> = Parameters<TElement['methods'][TMethod]>[0] extends undefined
  ? {}
  : Parameters<TElement['methods'][TMethod]>[0]

// Extract return type from a method function signature
export type ReturnTypeOf<
  TElement extends UIElementBase,
  TMethod extends MethodsOf<TElement>,
> = ReturnType<TElement['methods'][TMethod]>

// Simplified approach to validate element IDs
export type ValidateElementId<
  TTag extends keyof UIElementMap,
  TId extends string,
> = UIElementMap[TTag] extends { allowedIds: infer AllowedIds }
  ? TId extends AllowedIds & string
    ? TId
    : ErrorMessage<`Error: ID "${TId}" is not allowed for ${string & TTag} elements. Allowed IDs are: ${string & AllowedIds}`>
  : TId

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
  callback?: (
    result: InvocationResult<
      ReturnTypeOf<TElement, TMethod>,
      { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }
    >,
  ) => void,
): void {
  return lynx
    .createSelectorQuery()
    .select(selector)
    .invoke({
      method,
      params,
      success: (res: ReturnTypeOf<TElement, TMethod>) => {
        callback?.({ ok: true, data: res })
      },
      fail: (error: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => {
        callback?.({ ok: false, error })
      },
    })
    .exec()
}

/**
 * Creates a typed invoker with improved error messages
 * @param tag The UI element tag (e.g., 'input')
 * @param id The element ID to validate against allowed IDs
 */
export function createTypedInvoker<
  TTag extends keyof UIElementMap,
  TId extends string,
>(
  _tag: TTag,
  // Validate ID at the parameter level
  id: ValidateElementId<TTag, TId>,
) {
  type TElement = UIElementMap[TTag]

  return {
    invoke: <TMethod extends MethodsOf<TElement>>(
      method: TMethod,
      params?: ParamsOf<TElement, TMethod>,
      callback?: (
        result: InvocationResult<
          ReturnTypeOf<TElement, TMethod>,
          { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }
        >,
      ) => void,
    ) => {
      invokeExec(
        `#${id}`,
        method,
        params || ({} as ParamsOf<TElement, TMethod>),
        callback,
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
  callback?: (
    result: InvocationResult<
      ReturnTypeOf<TElement, TMethod>,
      { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }
    >,
  ) => void,
): void {
  invokeExec<TSelector, TElement, TMethod>(selector, method, params, callback)
}

// Simplified parameter type for invoker functions
export type InvokerParams<
  TTag extends keyof UIElementMap,
  TMethod extends MethodsOf<UIElementMap[TTag]>
> = ParamsOf<UIElementMap[TTag], TMethod>

// Simplified callback type for invoker functions
export type InvokerCallback<
  TTag extends keyof UIElementMap,
  TMethod extends MethodsOf<UIElementMap[TTag]>
> = (
  result: InvocationResult<
    ReturnTypeOf<UIElementMap[TTag], TMethod>,
    { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }
  >
) => void

/**
 * Creates a higher-order function that generates an element method invoker function
 * @param tag The UI element tag (e.g., 'input')
 * @param method The method to invoke on the element
 * @returns A function that takes an ID, params, and callback and invokes the method
 */
export function createMethodInvoker<
  TTag extends keyof UIElementMap,
  TMethod extends MethodsOf<UIElementMap[TTag]>
>(tag: TTag, method: TMethod) {
  return <TId extends string>(
    id: ValidateElementId<TTag, TId>,
    params?: InvokerParams<TTag, TMethod>,
    callback?: InvokerCallback<TTag, TMethod>
  ): void => {
    createTypedInvoker(tag, id).invoke(method, params, callback)
  }
}