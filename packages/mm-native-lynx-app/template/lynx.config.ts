import { defineConfig } from '@lynx-js/rspeedy'
import { pluginReactLynx } from '@lynx-js/react-rsbuild-plugin'
import fs from 'fs/promises'

const copyNativeAssets = async (
  platform: 'android' | 'ios',
  projectName: string,
) => {
  if (platform === 'ios') throw new Error(`Unsupported platform: ${platform}`)
  const langs = { ios: ['Swift', 'Objc'], android: ['Kotlin', 'Java'] }[
    platform
  ]

  const fromFolder = `dist`
  const toFolder = `${platform}/${projectName}/app/src/main/assets`
  await fs.cp(fromFolder, toFolder, { recursive: true })
}

function pluginCopyNativeAssets(
  platform: 'android' | 'ios',
  projectName: string,
) {
  return {
    name: 'lynxpo:copy-native-asset',
    setup(api) {
      api.onAfterBuild(
        async () => await copyNativeAssets(platform, projectName),
      )
    },
  }
}
export default defineConfig({
  plugins: [
    pluginCopyNativeAssets('android', 'Kotlin{{project_name}}'),
    pluginReactLynx(),
  ],
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
