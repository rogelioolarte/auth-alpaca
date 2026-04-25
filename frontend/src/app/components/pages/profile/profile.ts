import { Component, inject, OnInit } from '@angular/core';
import { AuthProvider } from '../../../models/user';
import { AuthenticationService } from '../../../auth/authentication-service';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-profile',
  imports: [],
  template: `
  @if(!!userInfo()) {
   <div class="profile-container" >
      <div class="profile-info">
        <div class="profile-name">
          <h2>You are logged in using {{ getAuthProviderDisplayName(authProvider) }}</h2>
          <p class="profile-field">Email: {{ userInfo()?.username }}</p>
          <p class="profile-field">User ID: {{ userInfo()?.id }}</p>
          @if(userInfo()?.profileId) {
            <p class="profile-field">Profile ID: {{ userInfo()?.profileId }}</p>
          }
          @if(userInfo()?.advertiserId) {
            <p class="profile-field">Advertiser ID: {{ userInfo()?.advertiserId }}</p>
          }
          <p class="profile-field">Authorities: </p>
          <ul>
            @for(authority of userInfo()?.authorities; track $index) {
              <li>{{ authority.authority }}</li>
            }
          </ul>
        </div>
      </div>
    </div>
  }
 
  `,
  styles: ``,
})
export class Profile implements OnInit {
  public authProvider = AuthProvider.provider
  private authService = inject(AuthenticationService)
  private route = inject(ActivatedRoute)
  public readonly userInfo = toSignal(this.authService.getUserInfo())

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.authProvider = params.get('authProvider') as AuthProvider;
    });
  }

  getAuthProviderDisplayName(authProvider: AuthProvider): string {
    switch (authProvider) {
      case AuthProvider.github:
        return 'GitHub';
      case AuthProvider.google:
        return 'Google';
      case AuthProvider.local:
        return 'Email/Password';
      default:
        return 'Unknown';
    }
  }

}
