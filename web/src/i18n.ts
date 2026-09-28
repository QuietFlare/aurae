import i18n from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import de from './locales/de.json'
import en from './locales/en.json'

// English is the source language. Every other language is one JSON file with the same keys.
export const supportedLanguages = ['en', 'de'] as const

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      de: { translation: de },
    },
    supportedLngs: supportedLanguages,
    fallbackLng: 'en',
    // React already escapes output, so i18next must not escape it a second time.
    interpolation: { escapeValue: false },
    detection: { order: ['localStorage', 'navigator'], caches: ['localStorage'] },
  })

export default i18n
