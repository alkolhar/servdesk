import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-account',
  imports: [MatButtonModule, MatCardModule, MatToolbarModule, TranslocoDirective],
  templateUrl: './account.html',
  styleUrl: './account.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Account {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
