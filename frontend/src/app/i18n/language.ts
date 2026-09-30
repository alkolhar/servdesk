export const AVAILABLE_LANGUAGES = ['en', 'de'] as const;
export type Language = (typeof AVAILABLE_LANGUAGES)[number];
export const FALLBACK_LANGUAGE: Language = 'en';

/**
 * The first of the browser's preferred languages servdesk speaks, matched on the primary subtag
 * (`de-CH` is German), else English (ADR-0003). A per-user override comes with My account's
 * language setting.
 */
export function detectLanguage(preferred: readonly string[] = navigator.languages): Language {
  for (const tag of preferred) {
    const primary = tag.toLowerCase().split('-')[0];
    const match = AVAILABLE_LANGUAGES.find((language) => language === primary);
    if (match) {
      return match;
    }
  }
  return FALLBACK_LANGUAGE;
}
