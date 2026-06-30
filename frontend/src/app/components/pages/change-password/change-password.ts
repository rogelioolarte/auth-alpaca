import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService } from '@app/api/user-service';
import { ChangePassword } from '@app/models/user';

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
],
  templateUrl: './change-password.html',
  styleUrl: './change-password.css',
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly snackBar = inject(MatSnackBar);

  public submitting = signal(false);

  passwordForm: FormGroup = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(200)]],
    reNewPassword: ['', Validators.required],
  }, {
    validators: this.passwordMatchValidator,
  });

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
          this.snackBar.open('Password changed successfully', 'Close', { duration: 3000 });
          this.passwordForm.reset();
          this.submitting.set(false);
        },
        error: (err) => {
          const errorMessage = err.error?.message || 'Error changing password';
          this.snackBar.open(errorMessage, 'Close', { duration: 3000 });
          this.submitting.set(false);
        },
      });
    }
  }
}
