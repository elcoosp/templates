import { defineConfig } from '@rslib/core'
import { createRsLibConfig } from '@elcoosp-configs/rslib'

export default defineConfig(async () => createRsLibConfig({ preset: 'dual' }))
