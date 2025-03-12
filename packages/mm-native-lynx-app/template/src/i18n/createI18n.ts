/**
 * Internationalization (i18n) module factory for dynamic locale loading
 *
 * @module i18nFactory
 * @description Provides a configurable factory function to create i18next instances
 * with Webpack-powered dynamic locale loading. Supports multiple translation formats
 * with extendable configuration.
 */

import i18next from 'i18next'
import type { i18n } from 'i18next'

/**
 * Translation format configuration registry
 * @constant {Object} translationsFormats
 * @property {Object} json - Configuration for JSON translation files
 * @property {RegExp} json.regExp - Webpack context regex for file matching
 * @property {RegExp} json.matcher - Regex to extract locale codes from file paths
 */
const translationsFormats = {
  json: {
    fileMatch: /\.json$/,
    localeCodeMatch: /\/([^/]+)\.json$/,
  },
} as const

/** Supported translation format types */
type Format = keyof typeof translationsFormats

/**
 * Creates a configured i18next instance with dynamic locale loading
 * @template T - Translation schema type (defaults to generic string map)
 *
 * @param {Format} format - Translation file format to use from translationsFormats
 * @returns {i18n} Configured i18next instance
 *
 * @throws {Error} If Webpack context fails to load translation files
 *
 * @example
 * // Basic usage with JSON translations
 * const i18n = createI18n('json')
 *
 * @example
 * // Usage with TypeScript type safety
 * type AppTranslations = { welcome: string; buttons: { submit: string }}
 * const i18n = createI18n<AppTranslations>('json')
 *
 * @remarks
 * - Requires Webpack context API for dynamic file loading
 * - Uses i18next compatibility mode v3 for environments without Intl.PluralRules
 * - Locale files should be stored in /locales directory at the same level
 */
export function createI18n<
  T extends Record<string, string> = Record<string, string>,
>(format: Format): i18n {
  const localI18nInstance: i18n = i18next.createInstance()

  // Configure Webpack dynamic import context for translation files
  const localesContext = import.meta.webpackContext('./locales', {
    recursive: false,
    regExp: translationsFormats[format].fileMatch,
  })

  // Initialize i18next instance with resolved translations
  localI18nInstance.init({
    lng: 'en',
    compatibilityJSON: 'v3', // Required for Lynx compatibility
    resources: Object.fromEntries(
      localesContext.keys().map((key) => {
        const localeCode =
          key.match(translationsFormats[format].localeCodeMatch)?.[1] || key
        return [localeCode, { translation: localesContext(key) as T }]
      }),
    ),
  })

  return localI18nInstance
}
