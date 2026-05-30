import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { catchError, retry, throwError, timer } from 'rxjs';

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
      let errorMessage: string;

      if (error.error instanceof Error) {
        errorMessage = `Client error: ${error.error.message}`;
      } else {
        errorMessage = evaluateErrorStatus(error)
      }

      if (error.status === 401) {
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

export const evaluateErrorStatus = (error: HttpErrorResponse): string => {
  switch (error.status) {
    case 401:
      return 'Session expired. Please log in again.';
    case 403:
      return 'You do not have permission to access this resource.';
      break;
    case 404:
      return 'The requested resource was not found.';
      break;
    case 500:
      return 'Internal server error. Please try again later.';
      break;
    case 0:
      return 'Unable to connect to the server. Check your connection.';
      break;
    default:
      return error.error?.message || `Error ${error.status}: ${error.status}`;
  }
}