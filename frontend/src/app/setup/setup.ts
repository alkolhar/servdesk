import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { AuthService } from '../auth/auth.service';
import { SetupService } from './setup.service';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmation = group.get('passwordConfirmation')?.value;
  return password === confirmation ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-setup',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    TranslocoDirective,
  ],
  templateUrl: './setup.html',
  styleUrl: './setup.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Setup {
  private readonly setup = inject(SetupService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      username: ['', Validators.required],
      // asked twice: this is the only administrator, and there is no password reset yet
      password: ['', Validators.required],
      passwordConfirmation: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  /** A translation key, never server text. */
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { name, email, username, password } = this.form.getRawValue();
    try {
      await this.setup.complete({ name, email, username, password });
    } catch (error) {
      this.submitting.set(false);
      if (error instanceof HttpErrorResponse && error.status === 409) {
        // someone else finished setup first: this account was not created
        this.setup.alreadyDone();
        await this.router.navigate(['/login'], { queryParams: { notice: 'setupAlreadyDone' } });
        return;
      }
      this.error.set(
        error instanceof HttpErrorResponse && error.status === 400
          ? 'setup.error.invalid'
          : 'setup.error.unavailable',
      );
      return;
    }
    try {
      await this.auth.login(username, password);
      await this.router.navigateByUrl('/account');
    } catch {
      // the account exists; signing in is the only step left
      await this.router.navigateByUrl('/login');
    } finally {
      this.submitting.set(false);
    }
  }
}
