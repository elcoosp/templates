{% if files_only %}
import { expect, test, vi } from 'vitest'
{% else %}
import { expect, test } from 'vitest'
{% endif %}

test('index', async () => {
  {% if files_only %}
  const consoleSpy = vi.spyOn(console, 'error');
  const { ERROR_MESSAGE } = await import('../src/index');
  expect(consoleSpy).toHaveBeenCalledOnce();
  expect(consoleSpy).toHaveBeenLastCalledWith(ERROR_MESSAGE);
  {% endif %}
  expect(true).toBe(true)
})
