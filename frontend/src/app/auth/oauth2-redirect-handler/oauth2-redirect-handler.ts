import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationService } from '../authentication-service';
import { ToastrService } from 'ngx-toastr';
import { AuthProvider } from '../../models/user';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-oauth2-redirect-handler',
  imports: [],
  template: ` <p>oauth2-redirect-handler works!</p> `,
  styles: ``,
})
export class Oauth2RedirectHandler implements OnInit, OnDestroy {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthenticationService);
  private toastService = inject(ToastrService);
  private authProvider = AuthProvider.provider;
  private destroy = new Subject<void>();

  ngOnInit() {
    this.route.paramMap.subscribe(
      (params) => (this.authProvider = params.get('provider') as AuthProvider),
    );

    this.route.queryParams.subscribe((params) => {
      const code: string = params['code'];
      const error: string = params['error'];

      if (error) {
        this.toastService.error(error, 'Error while logging!');
        this.router.navigate(['/login'], {
          state: { from: this.router.routerState.snapshot.url, error: error },
        });
      } else if (code && !error) {
        this.authService
          .exchangeCode(code)
          .pipe(takeUntil(this.destroy))
          .subscribe(() => {
            this.authService.recoverStates();
            this.router.navigate(['/dashboard/profile'], {
              state: { from: this.router.routerState.snapshot.url },
            });
          });
      } else {
        this.toastService.error('Error while processing logging');
      }
    });
  }

  ngOnDestroy() {
    this.destroy.next();
    this.destroy.complete();
  }
}
