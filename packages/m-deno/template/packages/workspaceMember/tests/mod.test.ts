import { squared } from "../src/mod.ts";
import { assertEquals } from "jsr:@std/assert";

Deno.test("squared", () => {
  assertEquals(squared(1, 1), 2);
});
