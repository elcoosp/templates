import {
  type InvokerCallback,
  type InvokerParams,
  type ValidateElementId,
  createMethodInvoker,
  createTypedInvoker,
} from '@utils/typed-lynx'

// Extend the base types with component-specific definitions
declare module '@utils/typed-lynx' {
  // Define InputElement interface with method signatures
  interface InputElement extends UIElementBase {
    methods: {
      blur: () => void
      focus: () => void
      // You can add more methods as needed
    }
    // Define allowed IDs (direct union type for clarity)
    allowedIds: 'loginInput' | 'searchInput'
  }

  // Add InputElement to UIElementMap
  interface UIElementMap {
    input: InputElement
    // Add more elements as needed
  }
}

// Create method invokers for input elements
export const focusElement = createMethodInvoker('input', 'focus')
export const blurElement = createMethodInvoker('input', 'blur')

// Manual method for comparison (the old way)
/**
 * Creates an invoker specifically for input elements with validation
 * @param id The input element ID to validate against allowed IDs
 */
export function getInputInvoker<TId extends string>(
  // Validate ID directly at the parameter level
  id: ValidateElementId<'input', TId>,
) {
  return createTypedInvoker('input', id)
}

export function manualFocusElement<TId extends string>(
  inputId: ValidateElementId<'input', TId>,
  params?: InvokerParams<'input', 'focus'>,
  callback?: InvokerCallback<'input', 'focus'>,
): void {
  getInputInvoker(inputId).invoke('focus', params, callback)
}
