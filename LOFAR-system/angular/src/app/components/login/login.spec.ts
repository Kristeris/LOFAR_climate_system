import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Login } from './login';
import { AuthService } from '../../services/auth.service';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { vi } from 'vitest';

describe('Login Component', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authServiceSpy: any;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = {
      isLoggedIn: vi.fn().mockReturnValue(false),
      login: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should show error if submitting empty fields', () => {
    component.username = '';
    component.password = '';

    component.onSubmit();

    expect(component.errorMsg()).toBe(
      'Please enter both username and password.'
    );

    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should navigate to admin dashboard on successful ADMIN login', () => {
    component.username = 'adminUser';
    component.password = 'superSecret';

    authServiceSpy.login.mockReturnValue(
      of({
        username: 'adminUser',
        role: 'ADMIN'
      })
    );

    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith(
      'adminUser',
      'superSecret'
    );

    expect(component.loading()).toBeFalsy();

    expect(router.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('should navigate to home on successful USER login', () => {
    component.username = 'regularUser';
    component.password = 'secret';

    authServiceSpy.login.mockReturnValue(
      of({
        username: 'regularUser',
        role: 'USER'
      })
    );

    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith(
      'regularUser',
      'secret'
    );

    expect(component.loading()).toBeFalsy();

    expect(router.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('should display invalid credentials error on 401 response', () => {
    component.username = 'wrongUser';
    component.password = 'wrongPass';

    authServiceSpy.login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401 }))
    );

    component.onSubmit();

    expect(component.errorMsg()).toBe(
      'Invalid username or password.'
    );

    expect(component.loading()).toBeFalsy();
  });
});