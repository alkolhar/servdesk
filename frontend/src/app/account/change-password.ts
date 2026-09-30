import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  FormGroupDirective,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslocoDirective } from '@jsverse/transloco';
import { AuthService } from '../auth/auth.service';
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  passwordPolicy,
  passwordsMatch,
} from '../auth/password-policy';

/** Changing your own password (#102), on My account. */
@Component({
  selector: 'app-change-password',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    TranslocoDirective,
  ],
  templateUrl: './change-password.html',
  styleUrl: './change-password.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePassword {
  private readonly auth = inject(AuthService);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', passwordPolicy],
      newPasswordConfirmation: ['', Validators.required],
    },
    { validators: passwordsMatch('newPassword', 'newPasswordConfirmation') },
  );

  protected readonly passwordLength = { min: PASSWORD_MIN_LENGTH, max: PASSWORD_MAX_LENGTH };

  /** Translation keys, never server text. */
  protected readonly error = signal<string | null>(null);
  protected readonly changed = signal(false);
  protected readonly submitting = signal(false);

  protected async submit(formDirective: FormGroupDirective): Promise<void> {
    this.changed.set(false);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    try {
      const { currentPassword, newPassword } = this.form.getRawValue();
      await this.auth.changePassword(currentPassword, newPassword);
      // through the directive, so the fields don't come back flagged as "required"
      formDirective.resetForm();
      this.changed.set(true);
    } catch (error) {
      const status = error instanceof HttpErrorResponse ? error.status : 0;
      this.error.set(
        status === 403
          ? 'account.password.error.wrongCurrent'
          : status === 400
            ? 'account.password.error.invalid'
            : 'account.password.error.unavailable',
      );
    } finally {
      this.submitting.set(false);
    }
  }
}
