import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

/**
 * Auth interceptor — attaches Bearer JWT token to every outbound request.
 * Uses Angular 17 functional interceptor pattern (no class needed).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }
  return next(req);
};

/**
 * Correlation ID interceptor — injects X-Correlation-Id on every request
 * so Spring Boot's CorrelationIdFilter picks it up for distributed tracing.
 */
export const correlationInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = uuidv4();
  req = req.clone({
    setHeaders: { 'X-Correlation-Id': correlationId },
  });
  return next(req);
};

/**
 * Global error interceptor — handles 401 (redirect to login),
 * 403 (forbidden toast), 5xx (generic error message).
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          localStorage.removeItem('access_token');
          router.navigate(['/login']);
          break;
        case 403:
          console.error('Access denied:', req.url);
          break;
        case 0:
          console.error('Network error — check your connection or backend status');
          break;
        default:
          console.error(`HTTP ${error.status}: ${error.message}`);
      }
      return throwError(() => error);
    })
  );
};
