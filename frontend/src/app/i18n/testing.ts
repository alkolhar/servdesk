import { TranslocoTestingModule, TranslocoTestingOptions } from '@jsverse/transloco';
import de from '../../../public/i18n/de.json';
import en from '../../../public/i18n/en.json';

/** The real translation files, preloaded, so a spec asserts on what a user actually reads. */
export function translocoTesting(options: TranslocoTestingOptions = {}) {
  return TranslocoTestingModule.forRoot({
    langs: { en, de },
    translocoConfig: { availableLangs: ['en', 'de'], defaultLang: 'en' },
    preloadLangs: true,
    ...options,
  });
}
