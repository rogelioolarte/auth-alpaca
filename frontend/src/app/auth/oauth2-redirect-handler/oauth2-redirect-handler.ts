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
export class Oauth2RedirectHandler implements OnInit, OnDestroy {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthenticationService);
  private readonly pkceService = inject(PkceService);
  private toastService = inject(ToastrService);
  private authProvider = AuthProvider.provider;
  private destroy = new Subject<void>();
  private clientID = toSignal(this.authService.getClientID());

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

  ngOnDestroy() {
    this.destroy.next();
    this.destroy.complete();
  }
}
