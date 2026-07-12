import { Component, inject, signal, DestroyRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { CommonModule } from '@angular/common';
import { Role } from '@app/models/role';
import { UserRequest } from '@app/models/user';
import { UserService } from '@app/api/user-service';
import { AuthenticationService } from '@app/auth/authentication-service';
import { ToastrService } from 'ngx-toastr';
import { handleBackendFormErrors, setupServerErrorClearing } from '@app/shared/utils/form-error-handler.util';

export interface UserDialogData {
  mode: 'create' | 'edit';
  user?: {
    id: string;
    email: string;
    roles?: Role[];
  };
  availableRoles?: Role[];
}

@Component({
  selector: 'app-user-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <h2>{{ data.mode === 'create' ? 'Create User' : 'Edit User' }}</h2>
        <button mat-icon-button (click)="onCancel()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <form [formGroup]="userForm" (ngSubmit)="onSubmit()">
        <mat-dialog-content>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email</mat-label>
            <input
              matInput
              formControlName="email"
              type="email"
              placeholder="user@example.com"
              [readonly]="data.mode === 'edit'"
            />
            @if (userForm.get('email')?.hasError('required')) {
              <mat-error>Email is required</mat-error>
            }
            @if (userForm.get('email')?.hasError('email')) {
              <mat-error>Please enter a valid email</mat-error>
            }
            @if (userForm.get('email')?.hasError('serverError')) {
              <mat-error>{{ userForm.get('email')?.getError('serverError') }}</mat-error>
            }
          </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input
                matInput
                formControlName="password"
                [type]="hidePassword() ? 'password' : 'text'"
                placeholder="Temporary password"
              />
              <button
                mat-icon-button
                matSuffix
                type="button"
                (click)="hidePassword.set(!hidePassword())"
                [attr.aria-label]="hidePassword() ? 'Show password' : 'Hide password'"
                [attr.aria-pressed]="!hidePassword()"
              >
                <mat-icon>{{ hidePassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (userForm.get('password')?.hasError('required')) {
                <mat-error>Password is required</mat-error>
              }
              @if (userForm.get('password')?.hasError('minlength')) {
                <mat-error>Password must be at least 8 characters</mat-error>
              }
              @if (userForm.get('password')?.hasError('serverError')) {
                <mat-error>{{ userForm.get('password')?.getError('serverError') }}</mat-error>
              }
            </mat-form-field>


          @let currentRoles = data.user?.roles;
          @if (data.mode === 'edit' && currentRoles && currentRoles.length > 0) {
            <div class="current-roles">
              <span class="label">Current Roles:</span>
              <div class="roles-chips">
                @for (role of currentRoles; track role.id) {
                  <mat-chip>{{ role.name }}</mat-chip>
                }
              </div>
            </div>
          }

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ data.mode === 'edit' ? 'Add Roles' : 'Roles' }}</mat-label>
            <mat-select formControlName="roles" multiple>
              @for (role of data.availableRoles; track role.id) {
                <mat-option [value]="role.id">{{ role.name }}</mat-option>
              }
            </mat-select>
            @if (userForm.get('roles')?.hasError('serverError')) {
              <mat-error>{{ userForm.get('roles')?.getError('serverError') }}</mat-error>
            }
          </mat-form-field>
        </mat-dialog-content>

        <mat-dialog-actions align="end">
          <button mat-button type="button" (click)="onCancel()">Cancel</button>
          <button
            mat-flat-button
            color="primary"
            type="submit"
            [disabled]="userForm.invalid || saving()"
          >
            @if (saving()) {
              <mat-spinner diameter="20"></mat-spinner>
            } @else {
              {{ data.mode === 'create' ? 'Create' : 'Save' }}
            }
          </button>
        </mat-dialog-actions>
      </form>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        min-width: 28rem;
      }
      .dialog-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--space-4) var(--space-6);
        border-bottom: 1px solid var(--mat-sys-outline-variant);
        h2 {
          margin: 0;
          font-size: 1.25rem;
          font-weight: 500;
        }
      }
      mat-dialog-content {
        padding: var(--space-6);
      }
      .full-width {
        width: 100%;
        margin-bottom: var(--space-2);
      }
      .current-roles {
        margin-bottom: var(--space-4);
        padding: var(--space-3) var(--space-4);
        background: var(--mat-sys-surface-container-low);
        border-radius: var(--radius-lg);

        .label {
          display: block;
          font-size: 0.75rem;
          font-weight: 500;
          color: var(--mat-sys-on-surface-variant);
          margin-bottom: var(--space-2);
        }

        .roles-chips {
          display: flex;
          flex-wrap: wrap;
          gap: var(--space-1);
        }
      }
      mat-dialog-actions {
        padding: var(--space-4) var(--space-6);
        gap: var(--space-2);
      }
    `,
  ],
})
export class UserDialog {
  userForm: FormGroup;
  saving = signal(false);
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<UserDialog>);
  public data: UserDialogData = inject(MAT_DIALOG_DATA);
  public hidePassword = signal(true);
  private userService = inject(UserService);
  private authService = inject(AuthenticationService);
  private toastr = inject(ToastrService);
  private destroyRef = inject(DestroyRef);

  constructor() {
    // Get initial role IDs for edit mode
    const initialRoleIds = this.data.user?.roles?.map((r) => r.id) || [];

    this.userForm = this.fb.group({
      email: [this.data.user?.email || '', [Validators.required, Validators.email]],
      password: [
        '',
        this.data.mode === 'create'
          ? [Validators.required, Validators.minLength(8), Validators.maxLength(200)]
          : [Validators.minLength(8), Validators.maxLength(200)],
      ],
      roles: [initialRoleIds],
    });

    setupServerErrorClearing(this.userForm, this.destroyRef);
  }

  onSubmit() {
    if (this.userForm.valid) {
      const form: UserRequest = this.userForm.value;
      if (form.password == '') {
        form.password = undefined;
      }
      this.saving.set(true);

      if (this.data.mode === 'edit') {
        this.userService.updateUserById(this.data.user!.id, form).subscribe({
          next: () => {
            this.toastr.success('User updated');
            this.dialogRef.close(true);
          },
          error: (err) => {
            handleBackendFormErrors(err, this.userForm, this.toastr);
            this.saving.set(false);
          },
        });
      } else {
        this.userService.createUser(form).subscribe({
          next: () => {
            this.toastr.success('User created');
            this.authService.rotateTokens().subscribe();
            this.dialogRef.close(true);
          },
          error: (err) => {
            handleBackendFormErrors(err, this.userForm, this.toastr);
            this.saving.set(false);
          },
        });
      }
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
