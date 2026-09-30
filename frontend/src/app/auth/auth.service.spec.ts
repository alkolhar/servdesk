import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MeModel } from '../api/models';
import { AuthService } from './auth.service';

const ada: MeModel = {
  id: 1,
  name: 'Ada Admin',
  role: 'AGENT',
  email: 'ada@example.com',
  username: 'admin',
};

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('asks /api/me once and remembers the answer', async () => {
    const first = auth.currentUser();
    http.expectOne('/api/me').flush(ada);
    expect(await first).toEqual(ada);

    expect(await auth.currentUser()).toEqual(ada);
    http.expectNone('/api/me');
    expect(auth.me()).toEqual(ada);
  });

  it('treats a 401 from /api/me as logged out', async () => {
    const answer = auth.currentUser();
    http.expectOne('/api/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(await answer).toBeNull();
  });

  it('does not mistake a server error for being logged out', async () => {
    const answer = auth.currentUser();
    http.expectOne('/api/me').flush(null, { status: 500, statusText: 'Server Error' });
    await expect(answer).rejects.toMatchObject({ status: 500 });
  });

  it('logs in with JSON credentials, then loads who that is', async () => {
    const done = auth.login('admin', 'admin-password');
    const login = http.expectOne('/api/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual({ username: 'admin', password: 'admin-password' });
    login.flush(null, { status: 204, statusText: 'No Content' });
    await Promise.resolve();
    http.expectOne('/api/me').flush(ada);
    await done;
    expect(auth.me()).toEqual(ada);
  });

  it('forgets the person on logout', async () => {
    const loaded = auth.currentUser();
    http.expectOne('/api/me').flush(ada);
    await loaded;

    const done = auth.logout();
    http
      .expectOne({ method: 'POST', url: '/api/logout' })
      .flush(null, { status: 204, statusText: 'No Content' });
    await done;
    expect(auth.me()).toBeNull();
  });

  it('changes the password with PUT /api/me/password', async () => {
    const done = auth.changePassword('the-old-password', 'a-brand-new-password');
    const put = http.expectOne({ method: 'PUT', url: '/api/me/password' });
    expect(put.request.body).toEqual({
      currentPassword: 'the-old-password',
      newPassword: 'a-brand-new-password',
    });
    put.flush(null, { status: 204, statusText: 'No Content' });
    await done;
  });
});
