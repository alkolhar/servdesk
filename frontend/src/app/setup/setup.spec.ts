import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { translocoTesting } from '../i18n/testing';
import { Setup } from './setup';
import { SetupService } from './setup.service';

describe('Setup', () => {
  const complete = vi.fn<SetupService['complete']>();
  const alreadyDone = vi.fn<SetupService['alreadyDone']>();
  const login = vi.fn<AuthService['login']>();
  let navigate: ReturnType<typeof vi.fn>;
  let navigateByUrl: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    complete.mockReset();
    alreadyDone.mockReset();
    login.mockReset();
    await TestBed.configureTestingModule({
      imports: [Setup, translocoTesting()],
      providers: [
        provideRouter([]),
        { provide: SetupService, useValue: { complete, alreadyDone } },
        { provide: AuthService, useValue: { login } },
      ],
    }).compileComponents();
    const router = TestBed.inject(Router);
    navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true) as never;
    navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true) as never;
  });

  const valid = {
    name: 'Ada Admin',
    email: 'ada@example.com',
    username: 'admin',
    password: 'admin-password',
    passwordConfirmation: 'admin-password',
  };

  async function submit(values: Partial<typeof valid> = {}): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(Setup);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const fields = { ...valid, ...values };
    for (const [name, value] of Object.entries(fields)) {
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

  it('creates the first Agent, signs them in and shows My account', async () => {
    complete.mockResolvedValue();
    login.mockResolvedValue();

    await submit();

    expect(complete).toHaveBeenCalledWith({
      name: 'Ada Admin',
      email: 'ada@example.com',
      username: 'admin',
      password: 'admin-password',
    });
    expect(login).toHaveBeenCalledWith('admin', 'admin-password');
    expect(navigateByUrl).toHaveBeenCalledWith('/account');
  });

  it('refuses mismatched passwords without calling the server', async () => {
    const element = await submit({ passwordConfirmation: 'something else' });

    expect(complete).not.toHaveBeenCalled();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      "The passwords don't match.",
    );
  });

  it('holds the first password to the policy', async () => {
    const element = await submit({ password: 'too-short', passwordConfirmation: 'too-short' });

    expect(complete).not.toHaveBeenCalled();
    expect(element.textContent).toContain('12 to 72 characters');
  });

  it('sends a late arrival to the login screen with a notice', async () => {
    complete.mockRejectedValue(new HttpErrorResponse({ status: 409 }));

    await submit();

    expect(alreadyDone).toHaveBeenCalled();
    expect(login).not.toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { notice: 'setupAlreadyDone' },
    });
  });

  it('shows a translated error when the server rejects the details', async () => {
    complete.mockRejectedValue(new HttpErrorResponse({ status: 400 }));

    const element = await submit();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'The server rejected these details.',
    );
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('falls back to the login screen if signing in fails after the account exists', async () => {
    complete.mockResolvedValue();
    login.mockRejectedValue(new HttpErrorResponse({ status: 503 }));

    await submit();

    expect(navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
