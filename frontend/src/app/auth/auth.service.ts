import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ChangePasswordRequest, LoginRequest, MeModel } from '../api/models';

/**
 * The browser session (ADR-0005). The server holds it in an HttpOnly cookie the SPA never sees;
 * what the SPA knows is `GET /api/me` — a person when logged in, 401 when not.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  // undefined: not asked yet; null: asked, nobody is logged in
  private readonly current = signal<MeModel | null | undefined>(undefined);

  readonly me = this.current.asReadonly();

  /** The logged-in person, asking the server only the first time. */
  async currentUser(): Promise<MeModel | null> {
    if (this.current() === undefined) {
      await this.refresh();
    }
    return this.current() ?? null;
  }

  async refresh(): Promise<MeModel | null> {
    try {
      this.current.set(await firstValueFrom(this.http.get<MeModel>('/api/me')));
    } catch (error) {
      if (!(error instanceof HttpErrorResponse && error.status === 401)) {
        throw error;
      }
      this.current.set(null);
    }
    return this.current() ?? null;
  }

  /** Rejects with the HttpErrorResponse on failure; 401 means wrong credentials. */
  async login(username: string, password: string): Promise<void> {
    const body: LoginRequest = { username, password };
    await firstValueFrom(this.http.post<void>('/api/login', body));
    await this.refresh();
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post<void>('/api/logout', null));
    this.current.set(null);
  }

  /** Rejects with the HttpErrorResponse; 403 means the current password was wrong. */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    const body: ChangePasswordRequest = { currentPassword, newPassword };
    await firstValueFrom(this.http.put<void>('/api/me/password', body));
  }

  /** The server stopped recognising the session (idle timeout, restart, deactivation). */
  sessionEnded(): void {
    this.current.set(null);
  }
}
