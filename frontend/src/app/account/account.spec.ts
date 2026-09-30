import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { MeModel } from '../api/models';
import { AuthService } from '../auth/auth.service';
import { translocoTesting } from '../i18n/testing';
import { Account } from './account';

const ada: MeModel = {
  id: 1,
  name: 'Ada Admin',
  role: 'AGENT',
  email: 'ada@example.com',
  username: 'admin',
};

describe('Account', () => {
  async function render(language: 'en' | 'de'): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [
        Account,
        translocoTesting({
          translocoConfig: { availableLangs: ['en', 'de'], defaultLang: language },
        }),
      ],
      providers: [provideRouter([]), { provide: AuthService, useValue: { me: signal(ada) } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(Account);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows who is logged in and their role', async () => {
    const element = await render('en');

    expect(element.querySelector('[data-testid="name"]')?.textContent).toContain('Ada Admin');
    expect(element.querySelector('[data-testid="role"]')?.textContent).toContain('Agent');
    expect(element.querySelector('h1')?.textContent).toContain('My account');
  });

  it('speaks German', async () => {
    const element = await render('de');

    expect(element.querySelector('h1')?.textContent).toContain('Mein Konto');
    expect(element.textContent).toContain('Rolle');
  });
});
