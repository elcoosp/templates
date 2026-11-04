import { assertEquals } from "jsr:@std/assert@^1.0.8";
import { squared } from "../src/mod.ts";

Deno.test("squared", () => {
  assertEquals(squared(1, 1), 2);
});
