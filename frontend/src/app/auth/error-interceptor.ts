import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { catchError, retry, throwError, timer } from 'rxjs';

/**
 * HTTP error interceptor that retries transient failures and shows user-facing
 * error notifications via Toastr.
 *
 * Automatically retries requests that fail with status 503 (Service Unavailable)
 * or 0 (network error) up to 2 times with a 1-second backoff between attempts.
 *
 * Bypasses the global toast for field validation errors: when the response status
 * is 400 and the error body is an object without a `message` key, the error is
 * passed through to the calling component without a notification. A 401 status is
 * also silently forwarded without a toast, as it is handled by
 * {@link authInterceptor}. All other non-401 errors trigger a Toastr error
 * notification with a message derived from the response status.
 *
 * @param req - The outgoing HTTP request.
 * @param next - The next handler in the interceptor chain.
 * @returns An observable of the HTTP event stream.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastrService);

  return next(req).pipe(
    retry({
      count: 2,
      delay: (error, retryCount) => {
        if (error.status === 503 || error.status === 0) {
          return timer(retryCount * 1000);
        }
        throw error;
      }
    }),
    catchError((error: HttpErrorResponse) => {
      // --- VALIDATION LOGIC ---
      // Bypass global toast if:
      // 1. Body is empty/null (nothing to show)
      // 2. It's a field validation error: status 400, is an object, and DOES NOT have a 'message' key
      const hasNoBody = !error.error;
      const isFieldValidationError = 
        error.status === 400 && 
        typeof error.error === 'object' && 
        error.error !== null && 
        !error.error?.['message'];

      if (hasNoBody || isFieldValidationError) {
        if (isFieldValidationError) {
          console.warn(`[HTTP Validation Error] Bypassing global toast for field errors:`, error.error);
        }
        return throwError(() => error); // Pass error intact to the component
      }

      let errorMessage: string;

      if (error.error instanceof Error) {
        errorMessage = error.error.message;
      } else {
        errorMessage = evaluateErrorStatus(error)
      }

      if (error.status === 401) {
        return throwError(() => error);
      }

      if (error.status < 99) {
        return throwError(() => error);
      }

      toastService.error(errorMessage, 'Error');

      console.error(`[HTTP Error Log]`, {
        status: error.status,
        message: errorMessage,
        original: error
      });

      return throwError(() => error);
    })
  );
};

/**
 * Maps HTTP error status codes to user-facing, localized error messages.
 *
 * Handles specific statuses:
 * - `401` — session expired
 * - `403` — permission denied
 * - `404` — resource not found
 * - `500` — internal server error
 * - `0` — network connectivity failure
 *
 * Falls back to `error.error.message` or a generic status-based string for any
 * unhandled status code.
 *
 * @param error - The HTTP error response to evaluate.
 * @returns A user-facing error message string.
 */
export const evaluateErrorStatus = (error: HttpErrorResponse): string => {
  switch (error.status) {
    case 401:
      return 'Session expired. Please log in again.';
    case 403:
      return 'You do not have permission to access this resource.';
    case 404:
      return 'The requested resource was not found.';
    case 500:
      return 'Internal server error. Please try again later.';
    case 0:
      return 'Unable to connect to the server. Check your connection.';
    default:
      return error.error?.message || `Error ${error.status}: ${error.message}`;
  }
}