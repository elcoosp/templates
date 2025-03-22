import type { RsbuildPlugin } from '@rsbuild/core';

export type PluginExampleOptions = {
  foo?: string;
  bar?: boolean;
};

export const pluginExample = (
  options: PluginExampleOptions = {},
): RsbuildPlugin => ({
  name: '{{org}}:{{project_name}}',

  setup() {
    console.log('Hello Rsbuild!', options);
  },
});
