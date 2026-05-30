import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../api/auth-service';
import { AuthenticationService } from '../../../auth/authentication-service';
import { ToastrService } from 'ngx-toastr';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCardContent } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInput } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { AuthProvider } from '../../../models/user';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { addCodeChallenge, GITHUB_AUTH_URL, GOOGLE_AUTH_URL } from '../../../models/constants';
import { GoogleIcon } from '../../icons/google-icon/google-icon';
import { CommonModule } from '@angular/common';
import { PkceService } from '@app/auth/pkce-service';

@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatCardContent,
    MatInput,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinner,
    GoogleIcon,
  ],
  templateUrl: './login.html',
  styleUrl: `./login.css`,
})
export class Login implements OnInit {
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly apiAuthService = inject(AuthService);
  private readonly authService = inject(AuthenticationService);
  private readonly pkceService = inject(PkceService);
  private readonly toastService = inject(ToastrService);
  public readonly clientId = toSignal(this.authService.getClientID());
  public readonly ProviderGoogle = AuthProvider.google;

  private authProvider = AuthProvider.provider;
  public loginForm!: FormGroup;
  public submitting = signal(false);
  public errorMessage = signal('');
  public hidePassword = signal(true);

  ngOnInit() {
    this.loginForm = this.formBuilder.group({
      email: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      return;
    }
    if (!this.clientId()) {
      this.toastService.warning('Reload the App.');
      return;
    }

    this.submitting.set(true);
    this.apiAuthService.login(this.loginForm.getRawValue(), this.clientId() || '').subscribe({
      next: (data) => {
        this.authService.setAuthTokens(data, false);
        this.router.navigate(['/dashboard'], {
          state: { from: this.router.routerState.snapshot.url },
        });
        this.submitting.set(false);

      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(err.error.message || err.message);
        this.submitting.set(false);
      }
    });
  }

  async loginWithProvider(provider: AuthProvider) {
    const codeChallenge = await this.pkceService.generateCodeChallenge();
    this.authProvider = provider;
    switch (provider) {
      case AuthProvider.google:
        window.location.href = addCodeChallenge(GOOGLE_AUTH_URL, codeChallenge);
        break;
      case AuthProvider.github:
        window.location.href = addCodeChallenge(GITHUB_AUTH_URL, codeChallenge);
        break;
      default:
        this.toastService.error('UnKnown Provider');
    }
  }
}
