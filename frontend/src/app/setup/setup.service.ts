import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { SetupRequest, SetupStatus } from '../api/models';

/**
 * First-run setup: while no person exists, `/api/setup` is the only way in. Once setup is done it
 * stays done (the server refuses a second run with 409), so the answer is asked for once.
 */
@Injectable({ providedIn: 'root' })
export class SetupService {
  private readonly http = inject(HttpClient);

  private required: boolean | undefined;

  async isRequired(): Promise<boolean> {
    if (this.required === undefined) {
      const status = await firstValueFrom(this.http.get<SetupStatus>('/api/setup'));
      this.required = status.setupRequired ?? false;
    }
    return this.required;
  }

  /** Creates the first Agent. Rejects with the HttpErrorResponse on failure; 409 means already done. */
  async complete(request: SetupRequest): Promise<void> {
    await firstValueFrom(this.http.post('/api/setup', request));
    this.required = false;
  }

  /** Someone else finished setup first. */
  alreadyDone(): void {
    this.required = false;
  }
}
