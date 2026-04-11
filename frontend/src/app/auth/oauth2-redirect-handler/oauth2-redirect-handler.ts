import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth-service';
import { ToastrService } from 'ngx-toastr';
import { AuthProvider } from '../../models/user';

@Component({
  selector: 'app-oauth2-redirect-handler',
  imports: [],
  template: ` <p>oauth2-redirect-handler works!</p> `,
  styles: ``,
})
export class Oauth2RedirectHandler implements OnInit {

  private router = inject(Router)
  private route = inject(ActivatedRoute)
  private authService = inject(AuthService)
  private toastService = inject(ToastrService)
  private authProvider = AuthProvider.provider

  ngOnInit() {
    this.route.paramMap.subscribe(params => 
      this.authProvider = params.get('provider') as AuthProvider);
    this.route.queryParams.subscribe(params => {
      const error: string = params['error']
      if(error) {
        this.toastService.error(error, "Error!")
        this.router.navigate(['/login'], {
          state: { from: this.router.routerState.snapshot.url, error: error }
        })
      } else {
        this.authService.recoverStates()
        this.router.navigate(['/dashboard/profile', this.authProvider], { 
          state: { from: this.router.routerState.snapshot.url } 
        })
      }
    })
   
  }

  

}
