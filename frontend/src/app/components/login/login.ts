import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { APIAuthService } from '../../api/auth-service';
import { AuthService } from '../../auth/auth-service';
import { ToastrService } from 'ngx-toastr';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormField, MatLabel } from "@angular/material/form-field";
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from "@angular/material/card";
import { MatButton, MatIconButton } from "@angular/material/button";
import { MatInput } from "@angular/material/input";
import { AuthProvider } from '../../models/user';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { GITHUB_AUTH_URL, GOOGLE_AUTH_URL } from '../../models/constants';
import { GoogleIcon } from "../icons/google-icon/google-icon";


@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatFormField,
    MatCard,
    MatCardHeader,
    MatCardContent,
    MatIconButton,
    MatInput,
    MatButton,
    MatFormField,
    MatLabel,
    MatCardTitle,
    GoogleIcon
],
  templateUrl: './login.html',
  styles: ``,
})
export class Login implements OnInit {
  private router = inject(Router)
  private formBuilder = inject(FormBuilder)
  private apiAuthService = inject(APIAuthService)
  private authService = inject(AuthService)
  private toastService = inject(ToastrService)
  public readonly clientId = toSignal(this.authService.getClientID())
  public readonly ProviderGoogle = AuthProvider.google

  private authProvider = AuthProvider.provider
  loginForm!: FormGroup
  loading = signal(false)
  submitting = signal(false)
  errorMessage = signal("")

  ngOnInit() {
    this.loginForm = this.formBuilder.group({
      email: ['', Validators.required],
      password: ['', Validators.required]
    })
  }

  onSubmit() {
    if(this.loginForm.invalid) {
      return
    }
    if(!this.clientId()) {
      this.toastService.warning("Reload the App.")
      return
    }

    this.submitting.set(true)
    this.loading.set(true)
    this.apiAuthService.login(this.loginForm.getRawValue(), this.clientId() || "").subscribe({
      next: data => {
        this.authProvider = AuthProvider.local
        this.authService.setAuthTokens(data, false)
        this.router.navigate(['/dashboard/profile', this.authProvider],
          {state: { from: this.router.routerState.snapshot.url }})
        this.loading.set(false)
        this.submitting.set(false)
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(err.error.message || err.message)
        this.loading.set(false)
        this.submitting.set(false)
      },
    })
  }

  loginWithProvider(provider: AuthProvider) {
    switch(provider) {
      case AuthProvider.google:
        window.location.href = GOOGLE_AUTH_URL;
        break;
      case AuthProvider.github:
        window.location.href = GITHUB_AUTH_URL;
        break;
      default:
        this.toastService.error('UnKnown Provider')
    }
  }

}
