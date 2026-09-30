import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Translation, TranslocoLoader } from '@jsverse/transloco';

/** Translations are plain static files (`public/i18n/*.json`), served from the jar like any asset. */
@Injectable({ providedIn: 'root' })
export class TranslocoHttpLoader implements TranslocoLoader {
  private readonly http = inject(HttpClient);

  getTranslation(language: string) {
    return this.http.get<Translation>(`/i18n/${language}.json`);
  }
}
