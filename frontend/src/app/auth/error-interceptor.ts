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
        // Error del lado del cliente o de red
        errorMessage = `Client error: ${error.error.message}`;
      } else {
        // Error del lado del servidor (Backend)
        switch (error.status) {
          case 401:
            errorMessage = 'Session expired. Please log in again.';
            // Aquí podrías inyectar AuthService y hacer logout
            break;
          case 403:
            errorMessage = 'You do not have permission to access this resource.';
            break;
          case 404:
            errorMessage = 'The requested resource was not found.';
            break;
          case 500:
            errorMessage = 'Internal server error. Please try again later.';
            break;
          case 0:
            errorMessage = 'Unable to connect to the server. Check your connection.';
            break;
          default:
            errorMessage = error.error?.message || `Error ${error.status}: ${error.status}`;
        }
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