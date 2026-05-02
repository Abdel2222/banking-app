import { HttpInterceptorFn } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

const TOKEN_KEY = 'auth_token';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const isBrowser = typeof window !== 'undefined' && typeof localStorage !== 'undefined';

  console.log(`[TOKEN_INTERCEPTOR] Processing ${req.method} ${req.url}`);

  // ✅ Si la requête a DÉJÀ un header Authorization, on ne le remplace PAS
  if (req.headers.has('Authorization')) {
    console.log('[TOKEN_INTERCEPTOR] Authorization déjà présent, on laisse passer');
    return next(req).pipe(
      catchError(error => handleError(error, router, isBrowser))
    );
  }

  let token: string | null = null;

  if (isBrowser) {
    token = localStorage.getItem(TOKEN_KEY);
    console.log(`[TOKEN_INTERCEPTOR] Token status: ${token ? 'FOUND' : 'NOT_FOUND'}`);
  }

  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    console.log(`[HTTP DEBUG] ${authReq.method} ${authReq.url} AuthHeader: true`);
  } else {
    console.log(`[HTTP DEBUG] ${req.method} ${req.url} AuthHeader: false`);
  }

  return next(authReq).pipe(
    catchError(error => handleError(error, router, isBrowser))
  );
};

function handleError(error: any, router: Router, isBrowser: boolean) {
  console.error(`[TOKEN_INTERCEPTOR] HTTP error:`, error);

  if (error.status === 401) {
    console.warn('[TOKEN_INTERCEPTOR] Unauthorized (401) - clearing token and redirecting');
    if (isBrowser) {
      localStorage.removeItem(TOKEN_KEY);
    }
    router.navigate(['/auth']);
  } else if (error.status === 403) {
    console.warn('[TOKEN_INTERCEPTOR] Forbidden (403)');
  } else if (error.status === 500) {
    console.error('[TOKEN_INTERCEPTOR] Server error');
  }

  return throwError(() => error);
}