import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { translocoTesting } from '../i18n/testing';
import { ChangePassword } from './change-password';

describe('ChangePassword', () => {
  const changePassword = vi.fn<AuthService['changePassword']>();

  beforeEach(async () => {
    changePassword.mockReset();
    await TestBed.configureTestingModule({
      imports: [ChangePassword, translocoTesting()],
      providers: [{ provide: AuthService, useValue: { changePassword } }],
    }).compileComponents();
  });

  const valid = {
    currentPassword: 'the-old-password',
    newPassword: 'a-brand-new-password',
    newPasswordConfirmation: 'a-brand-new-password',
  };

  async function submit(values: Partial<typeof valid> = {}): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(ChangePassword);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    for (const [name, value] of Object.entries({ ...valid, ...values })) {
      const input = element.querySelector<HTMLInputElement>(`[formcontrolname="${name}"]`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('blur'));
    }
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();
    return element;
  }

  it('changes the password, confirms it and clears the form', async () => {
    changePassword.mockResolvedValue();

    const element = await submit();

    expect(changePassword).toHaveBeenCalledWith('the-old-password', 'a-brand-new-password');
    expect(element.querySelector('[role="status"]')?.textContent).toContain(
      'Your password has been changed.',
    );
    for (const input of Array.from(element.querySelectorAll('input'))) {
      expect(input.value).toBe('');
    }
  });

  it('says so when the current password is wrong', async () => {
    changePassword.mockRejectedValue(new HttpErrorResponse({ status: 403 }));

    const element = await submit();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'The current password is wrong.',
    );
    expect(element.querySelector('[role="status"]')).toBeNull();
  });

  it('holds the new password to 12 to 72 characters before asking the server', async () => {
    for (const newPassword of ['eleven-char', 'x'.repeat(73)]) {
      const element = await submit({ newPassword, newPasswordConfirmation: newPassword });
      expect(element.textContent).toContain('12 to 72 characters');
    }
    expect(changePassword).not.toHaveBeenCalled();
  });

  it('refuses a mismatched repetition', async () => {
    const element = await submit({ newPasswordConfirmation: 'something-else-entirely' });

    expect(changePassword).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      "The passwords don't match.",
    );
  });

  it('shows a server rejection of the new password', async () => {
    changePassword.mockRejectedValue(new HttpErrorResponse({ status: 400 }));

    const element = await submit();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'The server rejected the new password.',
    );
  });
});
