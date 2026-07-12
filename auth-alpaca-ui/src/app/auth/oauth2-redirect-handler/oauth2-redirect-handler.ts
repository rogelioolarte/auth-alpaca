import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationService } from '../authentication-service';
import { ToastrService } from 'ngx-toastr';
import { AuthProvider } from '../../models/user';
import { Subject, takeUntil } from 'rxjs';
import { AuthCode } from '@app/models/auth';
import { PkceService } from '../pkce-service';
import { toSignal } from '@angular/core/rxjs-interop';
import { getRedirectURI } from '@app/models/constants';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-oauth2-redirect-handler',
  imports: [MatProgressSpinner],
  template: `<div class="spinn" ><mat-spinner/></div>`,
  styles: `
    .spinn {
      display: grid;
      place-content: center;
      place-items: center;
      padding-top: 5rem
    }
  `,
})
/**
 * Handles the OAuth2 authorization code redirect from external providers.
 *
 * When a user authenticates via an external OAuth2 provider (Google, GitHub, etc.),
 * the provider redirects back to this component with an authorization code in the
 * query parameters. This component reads the code and error params, exchanges the
 * code for tokens using PKCE (Proof Key for Code Exchange), and navigates to the
 * dashboard on success or back to login on error.
 *
 * @implements OnInit
 * @implements OnDestroy
 */
export class Oauth2RedirectHandler implements OnInit, OnDestroy {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthenticationService);
  private readonly pkceService = inject(PkceService);
  private toastService = inject(ToastrService);
  private authProvider = AuthProvider.provider;
  private destroy = new Subject<void>();
  private clientID = toSignal(this.authService.getClientID());

  /**
   * Reads the OAuth2 redirect parameters and processes the authorization response.
   *
   * Extracts the provider from the route parameter, then reads the `code` and `error`
   * query parameters from the URL. On error: displays the error in a toast and
   * navigates to `/login` with the error state. On success: builds an {@link AuthCode}
   * with the PKCE code verifier and exchanges it via {@link AuthenticationService#exchangeCode},
   * then navigates to `/dashboard`. Clears the PKCE verifier from session storage
   * after the exchange attempt.
   */
  ngOnInit() {
    this.route.paramMap.subscribe(
      (params) => (this.authProvider = params.get('provider') as AuthProvider),
    );

    this.route.queryParams.subscribe((params) => {
      const code: string = params['code'];
      const error: string = params['error'];
      const authCode: AuthCode = {
        code,
        client_id: this.clientID() || "",
        code_verifier: this.pkceService.getCodeVerifier(),
        redirect_uri: getRedirectURI(this.authProvider)
      }

      if (error) {
        this.toastService.error(error, 'Error while logging!');
        this.router.navigate(['/login'], {
          state: { from: this.router.routerState.snapshot.url, error: error },
        });
      } else if (code && !error) {
        this.authService
          .exchangeCode(authCode)
          .pipe(takeUntil(this.destroy))
          .subscribe({
            next: () => {
              this.authService.recoverStates();
              this.router.navigate(['/dashboard'], {
                state: { from: this.router.routerState.snapshot.url },
              })
            },
            error: () => {
              this.router.navigate(['/dashboard']);
              this.toastService.error('Error while processing. Report the Error');
            }
          });
          this.pkceService.clear();
      } else {
        this.toastService.error('Error while processing logging');
        this.pkceService.clear();
      }
    });
  }

  /**
   * Cleans up active subscriptions by signalling the destroy Subject.
   *
   * Emits a value on the destroy Subject and completes it, which triggers
   * `takeUntil(this.destroy)` to unsubscribe from the ongoing auth exchange
   * observable.
   */
  ngOnDestroy() {
    this.destroy.next();
    this.destroy.complete();
  }
}
