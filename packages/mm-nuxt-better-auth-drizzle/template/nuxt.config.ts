// Nuxt configuration
// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-11-01',
  devtools: { enabled: true },
  runtimeConfig: {
    public: {
      org: '{{org}}',
      projectName: '{{project_name}}',
      // biome-ignore format: keep the user-provided description on one line
      description: '{{description}}',
    },
  },
  css: ['~/assets/css/main.css'],
})
