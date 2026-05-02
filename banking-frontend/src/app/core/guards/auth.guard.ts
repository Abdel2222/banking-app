import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  console.log(`[AUTH_GUARD] Checking access to: ${state.url}`);

  if (!auth.isLoggedIn()) {
    console.log('[AUTH_GUARD] User not authenticated, redirecting to /auth');
    router.navigate(['/auth']);
    return false;
  }

  console.log('[AUTH_GUARD] Access granted');
  return true;
};
