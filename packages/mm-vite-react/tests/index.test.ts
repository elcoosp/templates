import { expect, test, vi } from 'vitest';

test('index', async () => {
  const consoleSpy = vi.spyOn(console, 'error');
  const { ERROR_MESSAGE } = await import('../src/index');
  expect(consoleSpy).toHaveBeenCalledOnce();
  expect(consoleSpy).toHaveBeenLastCalledWith(ERROR_MESSAGE);
});
