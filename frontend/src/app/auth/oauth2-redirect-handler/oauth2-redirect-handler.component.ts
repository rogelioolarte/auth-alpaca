import {Component, OnInit} from '@angular/core';
import {AuthProvider} from "../../model/model";
import {ActivatedRoute, Router} from "@angular/router";
import {AuthService} from "../auth.service";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-oauth2-redirect-handler',
  standalone: true,
  imports: [],
  templateUrl: './oauth2-redirect-handler.component.html',
  styleUrl: './oauth2-redirect-handler.component.css'
})
export class Oauth2RedirectHandlerComponent implements OnInit {

  token!: string;
  error!: string;
  authProvider: AuthProvider = AuthProvider.provider;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private toastrService: ToastrService
  ) {
  }

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.authProvider = params.get('provider') as AuthProvider;
    })

    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
      this.error = params['error'];

      if (this.token) {
        this.authService.setAuthentication(this.token);
        this.router.navigate(
          ['/dashboard/profile', this.authProvider],
          {state: {from: this.router.routerState.snapshot.url}}
        )
      } else {
        this.toastrService.error(this.error, 'Error!');
        this.router.navigate(
          ['/login'],
          {state: {from: this.router.routerState.snapshot.url, error: this.error}});
      }
    })
  }

}
