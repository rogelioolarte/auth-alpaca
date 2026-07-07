import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { UserService } from '@app/api/user-service';
import { ChangePassword } from '@app/models/user';
import { ToastrService } from 'ngx-toastr';
import { DestroyRef } from '@angular/core';
import { handleBackendFormErrors, setupServerErrorClearing } from '@app/shared/utils/form-error-handler.util';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinner,
    MatIcon,
  ],
  templateUrl: './change-password.html',
  styleUrl: './change-password.css',
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly toastService = inject(ToastrService);
  private destroyRef = inject(DestroyRef);
  public submitting = signal(false);
  public hideCurrentPassword = signal(true);
  public hideNewPassword = signal(true);
  public hideRepeatPassword = signal(true);

  passwordForm: FormGroup = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(200)]],
    reNewPassword: ['', Validators.required],
  }, {
    validators: this.passwordMatchValidator,
  });

  constructor() {
    setupServerErrorClearing(this.passwordForm, this.destroyRef);
  }

  passwordMatchValidator(group: FormGroup) {
    const newPwd = group.get('newPassword')?.value;
    const reTypePwd = group.get('reNewPassword')?.value;
    return newPwd === reTypePwd ? null : { passwordMismatch: true };
  }

  onSubmit() {
    if (this.passwordForm.valid) {
      this.submitting.set(true);
      const request: ChangePassword = this.passwordForm.value;

      this.userService.changePassword(request).subscribe({
        next: () => {
          this.toastService.success('Password changed successfully');
          this.passwordForm.reset();
          this.submitting.set(false);
        },
        error: (err) => {
          handleBackendFormErrors(err, this.passwordForm, this.toastService);
          this.submitting.set(false);
        },
      });
    }
  }
}
