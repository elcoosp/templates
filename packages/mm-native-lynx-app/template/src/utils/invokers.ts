import { NODE_REF_INVOKE_ERROR_CODE } from '@lynx-js/types'
import {
  type ReturnTypeOf,
  type UIElementMap,
  createTagInvoker,
  invokeElementMethod,
} from '@utils/typed-lynx'


// Extend the base types with component-specific definitions
declare module '@utils/typed-lynx' {
  // Define InputElement interface with direct method signatures
  interface InputElement extends UIElementBase {
    tag: 'input'
    methods: {
      // Simple method
      blur: () => void,
      
      // Complex method with generics and multiple parameters
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
export function blurElement(
  inputId: string,
  success?: (res: ReturnTypeOf<UIElementMap['input'], 'blur'>) => void,
  fail?: (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => void,
): void {
  invokeElementMethod(`#${inputId}`, 'blur', {}, success, fail)
}


export function focusElement(
  inputId: string,
  success?: (res: ReturnTypeOf<UIElementMap['input'], 'focus'>) => void,
  fail?: (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => void,
): void {
  invokeElementMethod(`#${inputId}`, 'focus', {}, success, fail)
}
