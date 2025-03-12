import { defineConfig } from '@lynx-js/rspeedy'
import { pluginReactLynx } from '@lynx-js/react-rsbuild-plugin'
import { pluginQRCode } from '@lynx-js/qrcode-rsbuild-plugin'
import { pluginTailwindCSS } from "rsbuild-plugin-tailwindcss";
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
    pluginCopyNativeBundle('android', 'Java{{project_name}}'),
    pluginTailwindCSS()
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
