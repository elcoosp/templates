import { NODE_REF_INVOKE_ERROR_CODE } from '@lynx-js/types'
import {
  type ReturnTypeOf,
  type UIElementMap,
  createTagInvoker,
  invokeElementMethod,
} from '@utils/typed-lynx'

// Extend the base types with component-specific definitions
declare module '@utils/typed-lynx' {
  // Define InputElement interface
  interface InputElement extends UIElementBase {
    tag: 'input'
    methods: {
      focus: {
        params: {}
        returnType: { code: number; message?: string }
      }
      blur: {
        params: {}
        returnType: { code: number; message?: string }
      }
      // Add more methods as needed
    }
  }

  // Add InputElement to UIElementMap
  interface UIElementMap {
    input: InputElement
    // Add more elements as needed
  }
}

// Type-safe focus element helper
export function focusElement(
  inputId: string,
  success?: (res: ReturnTypeOf<UIElementMap['input'], 'focus'>) => void,
  fail?: (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => void,
): void {
  invokeElementMethod(`#${inputId}`, 'focus', {}, success, fail)
}

// Type-safe blur element helper
export function blurElement(
  inputId: string,
  success?: (res: ReturnTypeOf<UIElementMap['input'], 'blur'>) => void,
  fail?: (res: { code: NODE_REF_INVOKE_ERROR_CODE; data?: any }) => void,
): void {
  invokeElementMethod(`#${inputId}`, 'blur', {}, success, fail)
}

// Helper functions for common elements
export function getInputInvoker(id: string) {
  return createTagInvoker('input', id)
}

// Usage example:
/*
// Focus an input element
focusElement('myInput', 
  result => console.log('Focus successful', result.code), 
  error => console.error('Focus failed', error)
);

// Or use the tag-based invoker
const inputControl = getInputInvoker('myInput');
inputControl.invoke('focus', {}, 
  result => console.log('Focus successful', result.code)
);
*/
