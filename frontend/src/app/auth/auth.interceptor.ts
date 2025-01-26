import {HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest} from "@angular/common/http";
import {inject} from "@angular/core";
import {AuthService} from "./auth.service";
import {ToastrService} from "ngx-toastr";
import {ACCESS_TOKEN_HEADER_KEY} from "../model/constants";
import {catchError, throwError} from "rxjs";

export const authenticationInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);
  const toastrService = inject(ToastrService);

  const authToken = authService.getToken();

  if (authToken) {
    req = req.clone({
      setHeaders: {
        [ACCESS_TOKEN_HEADER_KEY] : `Bearer ${authToken}`,
      },
    });
  }

  return next(req)
    .pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 403) {
          authService.logout();
        }

        const errorMessage = JSON.stringify(error.error, null, '\t');
        toastrService.error(errorMessage, 'Error!').onHidden
          .subscribe(() => {
            authService.logout();
          });

        return throwError(() => error);
      })
    )

}
