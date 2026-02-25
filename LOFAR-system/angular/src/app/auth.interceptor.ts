import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Attaches credentials (session cookie) to every outgoing request.
 * On 401 Unauthorized, redirects the user to /login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  // Clone request to add withCredentials so cookies are sent
  const authReq = req.clone({ withCredentials: true });

  return next(authReq).pipe(
    catchError(err => {
      if (err.status === 401) {
        // Session expired or not authenticated
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};