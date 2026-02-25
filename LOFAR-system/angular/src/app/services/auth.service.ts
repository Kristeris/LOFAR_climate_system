import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

export interface AuthUser {
  username: string;
  role: 'ADMIN' | 'USER';
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiBase = 'http://localhost:8080';

  // Signals for reactive auth state
  currentUser = signal<AuthUser | null>(null);
  isLoggedIn = computed(() => this.currentUser() !== null);
  isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  constructor(private http: HttpClient, private router: Router) {
    // On app start, try to restore session from backend
    this.fetchCurrentUser().subscribe({
      error: () => {
        // Not authenticated — clear state
        this.currentUser.set(null);
      }
    });
  }

  /**
   * POST /login with form-encoded credentials (Spring Security default)
   */
  login(username: string, password: string): Observable<AuthUser> {
    const body = new URLSearchParams();
    body.set('username', username);
    body.set('password', password);

    return this.http.post<AuthUser>(
      `${this.apiBase}/api/auth/login`,
      body.toString(),
      {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        withCredentials: true
      }
    ).pipe(
      tap(user => this.currentUser.set(user))
    );
  }

  /**
   * POST /logout - invalidates the server session
   */
  logout(): void {
    this.http.post(`${this.apiBase}/logout`, {}, { withCredentials: true })
      .subscribe({
        complete: () => {
          this.currentUser.set(null);
          this.router.navigate(['/login']);
        },
        error: () => {
          // Force logout even if request fails
          this.currentUser.set(null);
          this.router.navigate(['/login']);
        }
      });
  }

  /**
   * GET /api/auth/me - restores session state on page refresh
   */
  fetchCurrentUser(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${this.apiBase}/api/auth/me`, {
      withCredentials: true
    }).pipe(
      tap(user => this.currentUser.set(user)),
      catchError(err => {
        this.currentUser.set(null);
        return throwError(() => err);
      })
    );
  }
}