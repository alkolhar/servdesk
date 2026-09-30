import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

/** The server's password policy (#89): 12 to 72 characters, nothing else. */
export const PASSWORD_MIN_LENGTH = 12;
export const PASSWORD_MAX_LENGTH = 72;

/** For a password being set. The server checks the same; this is just earlier feedback. */
export const passwordPolicy: ValidatorFn[] = [
  Validators.required,
  Validators.minLength(PASSWORD_MIN_LENGTH),
  Validators.maxLength(PASSWORD_MAX_LENGTH),
];

/** A form-group validator: `password` and `confirmation` must be equal. */
export function passwordsMatch(password: string, confirmation: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null =>
    group.get(password)?.value === group.get(confirmation)?.value
      ? null
      : { passwordMismatch: true };
}
