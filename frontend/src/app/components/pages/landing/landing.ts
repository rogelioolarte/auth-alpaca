import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthenticationService } from '@app/auth/authentication-service';

@Component({
  selector: 'app-landing',
  imports: [MatIcon, MatButtonModule, RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class Landing {
  private readonly authService = inject(AuthenticationService);
  public readonly isAuthenticated = toSignal(this.authService.isAuthenticated());
}
