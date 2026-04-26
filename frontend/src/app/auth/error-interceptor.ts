import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { catchError, retry, throwError, timer } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastrService)

  return next(req).pipe(
    retry({
      count: 2,
      delay: (error, retryCount) => {
        if (error.status === 503 || error.status === 0) {
          return timer(retryCount * 1000); // Wait 1s, then 2s

        }
        throw error; // Do not retry other errors
      }
    }),
    // 2. Error Capture and Handling
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected error has occurred';

      if (error.error instanceof ErrorEvent) {
        // Client-side error (e.g., local network problems)
        errorMessage = `Error: ${error.error.message}`;

      } else {
        switch (error.status) {
          case 401:
            errorMessage = 'Session expired. Please log in again.';
            break;
          case 403:
            errorMessage = 'You do not have permission to access this resource.';
            break;
          case 404:
            errorMessage = 'The requested resource does not exist.';
            break;
          case 500:
            errorMessage = 'Internal server error. Please try again later.';
            break;
          case 0:
            errorMessage = 'No connection to the server. Check your internet.';
            break;
          default:
            errorMessage = error.error?.message || `Error code: ${error.status}`;
        }
      }

      toastService.error(`${errorMessage}`)
      console.error(`[HTTP Error]: ${errorMessage}`, error);

      // Important: Re-throw the error so the component can handle it if needed
      return throwError(() => new Error(errorMessage));

    })

  );

};