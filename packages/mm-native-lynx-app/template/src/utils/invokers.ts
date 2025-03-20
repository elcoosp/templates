import { NODE_REF_INVOKE_ERROR_CODE } from '@lynx-js/types'
import {
  type InvocationResult,
  createTypedInvoker,
  type AllowedIdOf, 
} from '@utils/typed-lynx'

// Extend the base types with component-specific definitions
declare module '@utils/typed-lynx' {
  // Define InputElement interface with method signatures (removed tag property)
  interface InputElement extends UIElementBase {
    methods: {
      blur: () => void,
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

// Helper functions for common elements
// Now uses AllowedIdOf to restrict IDs at the type level
export function getInputInvoker<TId extends AllowedIdOf<'input'>>(id: TId) {
  return createTypedInvoker('input', id)
}

// Simple wrapper functions for common operations
// Now uses EnsureIdAllowed to enforce type constraints
export function blurElement<ID extends AllowedIdOf<'input'>>(
  inputId: ID,
  callback?: (result: InvocationResult<void, { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }>) => void,
): void {
  getInputInvoker(inputId).invoke('blur', undefined, callback)
}

export function focusElement<ID extends AllowedIdOf<'input'>>(
  inputId: ID,
  callback?: (result: InvocationResult<void, { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }>) => void,
) {
  getInputInvoker(inputId).invoke('focus', undefined, callback)
}

// Example usage with the new pattern
export function handleInputOperation(inputId: AllowedIdOf<'input'>): void {
  blurElement(inputId, (result) => {
    if (result.ok) {
      console.log('Blur operation successful');
      // Access result.data if needed
    } else {
      console.error('Blur operation failed:', result.error.code);
      // Handle the error appropriately
    }
  });
}