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
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { handleBackendFormErrors, setupServerErrorClearing } from '@app/shared/utils/form-error-handler.util';
import { DestroyRef } from '@angular/core';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatCardContent,
    MatInput,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinner,
  ],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register implements OnInit {
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly apiAuthService = inject(AuthService);
  private readonly authService = inject(AuthenticationService);
  private readonly toastService = inject(ToastrService);
  private destroyRef = inject(DestroyRef);

  public readonly clientId = toSignal(this.authService.getClientID());

  public registerForm!: FormGroup;
  public submitting = signal(false);
  public hidePassword = signal(true);
  public hideConfirmPassword = signal(true);

  ngOnInit() {
    this.registerForm = this.formBuilder.group(
      {
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(200)]],
        confirmPassword: ['', Validators.required],
      },
      { validators: this.passwordMatchValidator },
    );
    setupServerErrorClearing(this.registerForm, this.destroyRef);
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      return;
    }

    if (!this.clientId()) {
      this.toastService.warning('Reload the App.');
      return;
    }

    this.submitting.set(true);

    const { email, password } = this.registerForm.value;

    this.apiAuthService.register({ email, password }, this.clientId() || '').subscribe({
      next: (data) => {
        this.authService.setAuthTokens(data, false);
        this.authService.updateUserInfo().subscribe();
        this.toastService.success('Account created successfully!');
        this.router.navigate(['/dashboard']);
        this.submitting.set(false);
      },
      error: (err: HttpErrorResponse) => {
        handleBackendFormErrors(err, this.registerForm, this.toastService);
        this.submitting.set(false);
      },
    });
  }
}
