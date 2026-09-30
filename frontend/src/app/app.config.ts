import { DOCUMENT } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  isDevMode,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { TranslocoService, provideTransloco } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { sessionExpiryInterceptor } from './auth/session-expiry.interceptor';
import { AVAILABLE_LANGUAGES, FALLBACK_LANGUAGE, detectLanguage } from './i18n/language';
import { TranslocoHttpLoader } from './i18n/transloco-loader';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // HttpClient's built-in XSRF support echoes the XSRF-TOKEN cookie as X-XSRF-TOKEN on every
    // same-origin write — exactly what the server expects of a session (ADR-0005)
    provideHttpClient(withInterceptors([sessionExpiryInterceptor])),
    provideRouter(routes),
    provideTransloco({
      config: {
        availableLangs: [...AVAILABLE_LANGUAGES],
        defaultLang: detectLanguage(),
        fallbackLang: FALLBACK_LANGUAGE,
        missingHandler: { useFallbackTranslation: true },
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
    // load the active language before the first screen renders (no flash of raw keys), and keep
    // <html lang> in step with it for screen readers and hyphenation
    provideAppInitializer(() => {
      const transloco = inject(TranslocoService);
      const document = inject(DOCUMENT);
      transloco.langChanges$.subscribe((language) => (document.documentElement.lang = language));
      return firstValueFrom(transloco.load(transloco.getActiveLang()));
    }),
  ],
};
