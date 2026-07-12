import { Component, inject, model } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbar } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { AuthenticationService } from '@app/auth/authentication-service';
import { MatSidenavModule } from '@angular/material/sidenav';
import { map } from 'rxjs';
import { PlatformService } from '@app/services/platform-service';

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLink,
    MatToolbar,
    MatIconModule,
    MatMenuModule,
    MatButtonModule,
    MatSidenavModule
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthenticationService);
  private readonly platformService = inject(PlatformService);
  public readonly user = toSignal(this.authService.getUserInfo());
  public readonly isAdmin = toSignal(this.authService.isAdmin());
  public sidenavOpened = model(true);
  
  public isInDashboard = toSignal(
    this.platformService.actualRoute().pipe(map((i) => i.includes('/dashboard'))),
  );

  logout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  formatRole(role: string): string {
    return role.replace('ROLE_', '');
  }

  toggleSidenav() {
    this.sidenavOpened.update((v) => !v);
  }
}
