import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router, UrlTree, provideRouter } from '@angular/router';
import { SetupService } from './setup.service';
import { setupDoneGuard, setupPendingGuard } from './setup.guards';

describe('setup routing', () => {
  function run(guard: CanActivateFn, setupRequired: boolean): Promise<boolean | UrlTree> {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SetupService, useValue: { isRequired: async () => setupRequired } },
      ],
    });
    return TestBed.runInInjectionContext(() => guard(null!, null!) as Promise<boolean | UrlTree>);
  }

  function url(result: boolean | UrlTree): string | boolean {
    return result instanceof UrlTree ? TestBed.inject(Router).serializeUrl(result) : result;
  }

  it('sends every other screen to setup while it is pending', async () => {
    expect(url(await run(setupDoneGuard, true))).toBe('/setup');
  });

  it('lets every other screen through once setup is done', async () => {
    expect(url(await run(setupDoneGuard, false))).toBe(true);
  });

  it('opens the setup screen while it is pending', async () => {
    expect(url(await run(setupPendingGuard, true))).toBe(true);
  });

  it('sends the setup screen to login once setup is done', async () => {
    expect(url(await run(setupPendingGuard, false))).toBe('/login');
  });
});
