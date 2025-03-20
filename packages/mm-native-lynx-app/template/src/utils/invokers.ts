import { NODE_REF_INVOKE_ERROR_CODE } from '@lynx-js/types'
import {
  type InvocationResult,
  type ReturnTypeOf,
  type UIElementMap,
  createTagInvoker,
  invokeElementMethod,
  invokeElementMethodWithCallbacks
} from '@utils/typed-lynx'

// Extend the base types with component-specific definitions
declare module '@utils/typed-lynx' {
  // Define InputElement interface with direct method signatures
  interface InputElement extends UIElementBase {
    tag: 'input'
    methods: {
      // Simple method
      blur: () => void,
      focus: () => void
      // You can add more methods directly with their signatures
    }
  }

  // Add InputElement to UIElementMap
  interface UIElementMap {
    input: InputElement
    // Add more elements as needed
  }
}

// Helper functions for common elements
export function getInputInvoker(id: string) {
  return createTagInvoker('input', id)
}

// New style with result callback
export function blurElement(
  inputId: string,
  callback?: (result: InvocationResult<void>) => void,
): void {
  invokeElementMethod(`#${inputId}`, 'blur', {}, callback)
}

export function focusElement(
  inputId: string,
  callback?: (result: InvocationResult<void>) => void,
): void {
  invokeElementMethod(`#${inputId}`, 'focus', {}, callback)
}

// Legacy style with separate callbacks (for backward compatibility)
export function blurElementLegacy(
  inputId: string,
  success?: (res: ReturnTypeOf<UIElementMap['input'], 'blur'>) => void,
  fail?: (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => void,
): void {
  invokeElementMethodWithCallbacks(`#${inputId}`, 'blur', {}, success, fail)
}

export function focusElementLegacy(
  inputId: string,
  success?: (res: ReturnTypeOf<UIElementMap['input'], 'focus'>) => void,
  fail?: (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => void,
): void {
  invokeElementMethodWithCallbacks(`#${inputId}`, 'focus', {}, success, fail)
}

// Example usage with the new pattern
export function handleInputOperation(inputId: string): void {
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