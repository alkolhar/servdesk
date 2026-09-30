import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { AuthService } from '../auth/auth.service';

/** The frame around every screen a logged-in Agent sees: the toolbar, and the screen below it. */
@Component({
  selector: 'app-shell',
  imports: [MatButtonModule, MatToolbarModule, RouterLink, RouterOutlet, TranslocoDirective],
  template: `
    <ng-container *transloco="let t">
      <mat-toolbar>
        <a class="brand" routerLink="/account">servdesk</a>
        <span class="spacer"></span>
        <a mat-button routerLink="/account">{{ t('shell.account') }}</a>
        <button mat-button type="button" (click)="logout()">{{ t('shell.logout') }}</button>
      </mat-toolbar>
      <main class="page">
        <router-outlet />
      </main>
    </ng-container>
  `,
  styles: `
    .brand {
      color: inherit;
      text-decoration: none;
    }
    .spacer {
      flex: 1;
    }
    .page {
      max-width: 760px;
      margin: 24px auto;
      padding: 0 16px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
