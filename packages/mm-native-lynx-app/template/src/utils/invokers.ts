import { NODE_REF_INVOKE_ERROR_CODE } from '@lynx-js/types'
import {
  type InvocationResult,
  type ValidateElementId,
  createTypedInvoker,
} from '@utils/typed-lynx'

// Extend the base types with component-specific definitions
declare module '@utils/typed-lynx' {
  // Define InputElement interface with method signatures (removed tag property)
  interface InputElement extends UIElementBase {
    methods: {
      blur: () => void
      focus: () => void
      // You can add more methods as needed
    }
    // Optional: restrict to specific selectors at compile time
    allowedSelectors: ['#loginInput', '#searchInput'] // Example of restricting allowed selectors
  }

  // Add InputElement to UIElementMap
  interface UIElementMap {
    input: InputElement
    // Add more elements as needed
  }
}

/**
 * Creates an invoker specifically for input elements with validation
 * @param id The input element ID to validate against allowed selectors
 */
export function getInputInvoker<TId extends string>(
  // Validate ID directly at the parameter level
  id: ValidateElementId<'input', TId>,
) {
  return createTypedInvoker('input', id)
}

// Focus element with direct validation
export function focusElement<TId extends string>(
  inputId: ValidateElementId<'input', TId>,
  callback?: (
    result: InvocationResult<
      void,
      { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }
    >,
  ) => void,
): void {
  getInputInvoker(inputId).invoke('focus', undefined, callback)
}

// Blur element with direct validation
export function blurElement<TId extends string>(
  inputId: ValidateElementId<'input', TId>,
  callback?: (
    result: InvocationResult<
      void,
      { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }
    >,
  ) => void,
): void {
  getInputInvoker(inputId).invoke('blur', undefined, callback)
}
