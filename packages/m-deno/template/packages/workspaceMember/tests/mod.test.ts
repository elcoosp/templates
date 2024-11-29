import { assertEquals } from 'jsr:@std/assert'
import { squared } from '../src/mod.ts'

Deno.test('squared', () => {
  assertEquals(squared(1, 1), 2)
})
