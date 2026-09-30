import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { TranslocoDirective } from '@jsverse/transloco';
import { AuthService } from '../auth/auth.service';
import { ChangePassword } from './change-password';

@Component({
  selector: 'app-account',
  imports: [ChangePassword, MatCardModule, TranslocoDirective],
  templateUrl: './account.html',
  styleUrl: './account.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Account {
  protected readonly auth = inject(AuthService);
}
