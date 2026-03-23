import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';

/**
 * Auth guard — blocks unauthenticated users and redirects to /login.
 * Angular 17 functional guard — no class, no implements.
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token  = localStorage.getItem('access_token');

  if (token) return true;

  router.navigate(['/login']);
  return false;
};

/**
 * Role guard — checks that the authenticated user has one of the required roles.
 * Roles are stored in the decoded JWT payload under the 'roles' claim.
 * Usage in routes: data: { roles: ['ADMIN', 'CLINICIAN'] }
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router        = inject(Router);
  const requiredRoles = route.data['roles'] as string[] | undefined;

  if (!requiredRoles || requiredRoles.length === 0) return true;

  const token = localStorage.getItem('access_token');
  if (!token) {
    router.navigate(['/login']);
    return false;
  }

  try {
    const payload   = JSON.parse(atob(token.split('.')[1]));
    const userRoles = (payload['roles'] as string[]) ?? [];
    const hasRole   = requiredRoles.some(r => userRoles.includes(r));

    if (!hasRole) {
      router.navigate(['/dashboard']);
      return false;
    }
    return true;

  } catch {
    router.navigate(['/login']);
    return false;
  }
};
