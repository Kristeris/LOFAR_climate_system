import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/role';

/**
 * Single guard used on every protected route.
 *
 * How it works:
 *  1. If not logged in → redirect to /login (preserving returnUrl)
 *  2. If route has data.roles defined → check user's role is in that list
 *     - Not allowed → redirect to /sensors
 *  3. If no data.roles → any authenticated user is allowed
 *
 * Usage in routes:
 *   { path: 'admin',   ..., canActivate: [AuthGuard], data: { roles: [Role.Admin] } }
 *   { path: 'sensors', ..., canActivate: [AuthGuard], data: { roles: [Role.Admin, Role.User] } }
 */
export const AuthGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  // 1. Not logged in at all
  if (!auth.isLoggedIn()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  // 2. Check role requirement from route data
  const requiredRoles: Role[] | undefined = route.data['roles'];

  if (requiredRoles && requiredRoles.length > 0) {
    const userRole = auth.currentUser()?.role as Role;
    const hasRole  = requiredRoles.includes(userRole);

    if (!hasRole) {
      // Logged in but wrong role — send to default page
      router.navigate(['/sensors']);
      return false;
    }
  }

  return true;
};