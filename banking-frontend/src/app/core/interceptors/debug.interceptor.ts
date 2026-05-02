import { HttpInterceptorFn } from '@angular/common/http';

export const debugInterceptor: HttpInterceptorFn = (req, next) => {
  const hasAuth = req.headers.has('Authorization');
  if (req.url.includes('/api/')) {
    console.log('[HTTP DEBUG]', req.method, req.url, 'AuthHeader:', hasAuth);
  }
  return next(req);
};
