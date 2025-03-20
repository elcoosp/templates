import { defineConfig } from '@lynx-js/rspeedy'
import { pluginReactLynx } from '@lynx-js/react-rsbuild-plugin'
import { pluginQRCode } from '@lynx-js/qrcode-rsbuild-plugin'
import { pluginTailwindCSS } from 'rsbuild-plugin-tailwindcss'
import pluginAutoImport from 'unplugin-auto-import/rspack'
import fs from 'fs/promises'

const copyNativeBundle = async (
  platform: 'android' | 'ios',
  projectName: string,
) => {
  if (platform === 'ios') throw new Error(`Unsupported platform: ${platform}`)
  const fromFolder = `dist`
  const bundleName = 'main.lynx.bundle'
  const toFolder = `${platform}/${projectName}/app/src/main/assets`
  await fs.copyFile(`${fromFolder}/${bundleName}`, `${toFolder}/${bundleName}`)
}

function pluginCopyNativeBundle(
  platform: 'android' | 'ios',
  projectName: string,
) {
  return {
    name: 'lynxpo:copy-native-bundle',
    setup(api) {
      api.onAfterBuild(
        async () => await copyNativeBundle(platform, projectName),
      )
    },
  }
}
export default defineConfig({
  plugins: [
    pluginReactLynx(),
    pluginQRCode(),
    pluginCopyNativeBundle('android', 'Kotlin{{project_name}}'),
    pluginTailwindCSS(),
  ],
  tools: {
    rspack: {
      plugins: [
        pluginAutoImport({
          dts: './src/auto-imports.d.ts',
          dirs: ['./src/i18n'],

          imports: [
            'react-router',
            {
              // TODO: PR https://github.com/unplugin/unplugin-auto-import/tree/main/src/presets
              '@lynx-js/react': [
                'useState',
                'useCallback',
                'useMemo',
                'useEffect',
                'useRef',
                'useContext',
                'useReducer',
                'useImperativeHandle',
                'useDebugValue',
                'useSyncExternalStore',
                'lazy',
                'memo',
                'createRef',
                'forwardRef',
              ],
            },
          ],
        }),
      ],
    },
  },
  environments: {
    web: {},
    lynx: {},
  },
  source: {
    alias: {
      '@assets': './assets',
    },
  },
})
