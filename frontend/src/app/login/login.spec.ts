import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { translocoTesting } from '../i18n/testing';
import { Login } from './login';

describe('Login', () => {
  const login = vi.fn<AuthService['login']>();

  beforeEach(async () => {
    login.mockReset();
    await TestBed.configureTestingModule({
      imports: [Login, translocoTesting()],
      providers: [provideRouter([]), { provide: AuthService, useValue: { login } }],
    }).compileComponents();
  });

  async function submit(username: string, password: string): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    const inputs = element.querySelectorAll('input');
    inputs[0].value = username;
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = password;
    inputs[1].dispatchEvent(new Event('input'));
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();
    return element;
  }

  it('goes to My account after a successful login', async () => {
    login.mockResolvedValue();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await submit('admin', 'admin-password');

    expect(login).toHaveBeenCalledWith('admin', 'admin-password');
    expect(navigate).toHaveBeenCalledWith('/account');
  });

  it('shows a translated error for wrong credentials', async () => {
    login.mockRejectedValue(new HttpErrorResponse({ status: 401 }));

    const element = await submit('admin', 'wrong');

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Wrong username or password.',
    );
  });

  it('tells a server failure apart from wrong credentials', async () => {
    login.mockRejectedValue(new HttpErrorResponse({ status: 503 }));

    const element = await submit('admin', 'admin-password');

    expect(element.querySelector('[role="alert"]')?.textContent).toContain("can't be reached");
  });

  it('does not send an empty form', async () => {
    await submit('', '');

    expect(login).not.toHaveBeenCalled();
  });
});

describe('Login notices', () => {
  async function render(notice: string): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [Login, translocoTesting()],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login: vi.fn() } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ notice }) } },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('says why the user was sent here', async () => {
    const element = await render('setupAlreadyDone');

    expect(element.querySelector('[role="status"]')?.textContent).toContain(
      'servdesk has already been set up.',
    );
  });

  it('ignores notices it does not know', async () => {
    for (const notice of ['whatever', 'constructor', '__proto__']) {
      TestBed.resetTestingModule();
      const element = await render(notice);
      expect(element.querySelector('[role="status"]')).toBeNull();
    }
  });
});
