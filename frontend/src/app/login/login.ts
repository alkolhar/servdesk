import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    TranslocoDirective,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  /** A translation key, never server text: the ProblemDetail's detail is English-only. */
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    try {
      const { username, password } = this.form.getRawValue();
      await this.auth.login(username, password);
      await this.router.navigateByUrl('/account');
    } catch (error) {
      this.error.set(
        error instanceof HttpErrorResponse && error.status === 401
          ? 'login.error.invalidCredentials'
          : 'login.error.unavailable',
      );
    } finally {
      this.submitting.set(false);
    }
  }
}
