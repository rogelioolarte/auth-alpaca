import { Component, computed, inject, signal } from '@angular/core';
import { AuthenticationService } from '../../../auth/authentication-service';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { Navbar } from '../navbar/navbar';
import { MatIcon } from '@angular/material/icon';
import { map } from 'rxjs';
import { PlatformService } from '@app/services/platform-service';

@Component({
  selector: 'app-base',
  imports: [
    RouterOutlet,
    MatMenuModule,
    MatSidenavModule,
    MatListModule,
    Navbar,
    MatIcon,
    RouterLink,
  ],
  templateUrl: './base.html',
  styleUrl: './base.css',
})
export class Base {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthenticationService);
  private readonly platformService = inject(PlatformService);
  public readonly isAuthenticated = toSignal(this.authService.isAuthenticated());
  public readonly user = toSignal(this.authService.getUserInfo());
  public readonly isAdmin = toSignal(this.authService.isAdmin());
  public sidenavOpened = signal(true);
  public isInDashboard = toSignal(
    this.platformService.actualRoute().pipe(map((i) => i.includes('/dashboard'))),
  );
  public readonly myPublicAdvertiserId = computed(() =>this.user()?.advertiserId || '');

  logout() {
    this.authService.logout().subscribe(() => this.router.navigateByUrl('/login'));
  }

  toggleSidenav() {
    this.sidenavOpened.update((v) => !v);
  }

}
