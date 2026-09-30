import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SetupService } from './setup.service';

describe('SetupService', () => {
  let setup: SetupService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    setup = TestBed.inject(SetupService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('asks the server once', async () => {
    const first = setup.isRequired();
    http.expectOne('/api/setup').flush({ setupRequired: true });
    expect(await first).toBe(true);

    expect(await setup.isRequired()).toBe(true);
    http.expectNone('/api/setup');
  });

  it('is no longer required once completed', async () => {
    const first = setup.isRequired();
    http.expectOne('/api/setup').flush({ setupRequired: true });
    await first;

    const done = setup.complete({
      name: 'Ada',
      email: 'ada@example.com',
      username: 'admin',
      password: 'secret',
    });
    const post = http.expectOne({ method: 'POST', url: '/api/setup' });
    expect(post.request.body).toEqual({
      name: 'Ada',
      email: 'ada@example.com',
      username: 'admin',
      password: 'secret',
    });
    post.flush({}, { status: 201, statusText: 'Created' });
    await done;

    expect(await setup.isRequired()).toBe(false);
  });
});
