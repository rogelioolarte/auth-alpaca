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
import { Permission } from '@app/models/permission';
import { RoleService } from '@app/api/role-service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ToastrService } from 'ngx-toastr';
import { handleBackendFormErrors, setupServerErrorClearing } from '@app/shared/utils/form-error-handler.util';
import { RoleRequest } from '@app/models/role';

export interface RoleDialogData {
  mode: 'create' | 'edit';
  role?: {
    id: string;
    name: string;
    description: string;
    permissions?: Permission[];
  };
  availablePermissions?: Permission[];
}

@Component({
  selector: 'app-role-dialog',
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
        <h2>{{ data.mode === 'create' ? 'Create Role' : 'Edit Role' }}</h2>
        <button mat-icon-button (click)="onCancel()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <form [formGroup]="roleForm" (ngSubmit)="onSubmit()">
        <mat-dialog-content>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" placeholder="e.g., ROLE_USER" />
            @if (roleForm.get('name')?.hasError('required')) {
              <mat-error>Name is required</mat-error>
            }
            @if (roleForm.get('name')?.hasError('serverError')) {
              <mat-error>{{ roleForm.get('name')?.getError('serverError') }}</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <textarea
              matInput
              formControlName="description"
              rows="2"
              placeholder="Role description..."
            ></textarea>
            @if (roleForm.get('description')?.hasError('serverError')) {
              <mat-error>{{ roleForm.get('description')?.getError('serverError') }}</mat-error>
            }
          </mat-form-field>

          @let currentPerms = data.role?.permissions;
          @if (data.mode === 'edit' && currentPerms && currentPerms.length > 0) {
            <div class="current-permissions">
              <span class="section-label">Current Permissions:</span>
              <div class="permissions-chips">
                @for (perm of currentPerms; track perm.id) {
                  <mat-chip>{{ perm.name }}</mat-chip>
                }
              </div>
            </div>
          }

          @if (data.availablePermissions && data.availablePermissions.length > 0) {
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>{{ data.mode === 'edit' ? 'Add Permissions' : 'Permissions' }}</mat-label>
              <mat-select formControlName="permissions" multiple>
                @for (perm of data.availablePermissions; track perm.id) {
                  <mat-option [value]="perm.id">{{ perm.name }}</mat-option>
                }
              </mat-select>
              @if (roleForm.get('permissions')?.hasError('serverError')) {
                <mat-error>{{ roleForm.get('permissions')?.getError('serverError') }}</mat-error>
              }
            </mat-form-field>
          } @else {
            <div class="no-permissions">
              <mat-icon>info</mat-icon>
              <span>No permissions available. Create permissions first.</span>
            </div>
          }
        </mat-dialog-content>

        <mat-dialog-actions align="end">
          <button mat-button type="button" (click)="onCancel()">Cancel</button>
          <button
            mat-flat-button
            color="primary"
            type="submit"
            [disabled]="roleForm.invalid || saving()"
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
        min-width: 30rem;
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
      .current-permissions {
        margin-bottom: var(--space-4);
        padding: var(--space-3) var(--space-4);
        background: var(--mat-sys-surface-container-low);
        border-radius: var(--radius-lg);

        .section-label {
          display: block;
          font-size: 0.75rem;
          font-weight: 500;
          color: var(--mat-sys-on-surface-variant);
          margin-bottom: var(--space-2);
        }

        .permissions-chips {
          display: flex;
          flex-wrap: wrap;
          gap: var(--space-1);
        }
      }
      .no-permissions {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        padding: var(--space-3) var(--space-4);
        background: var(--mat-sys-surface-container-low);
        border-radius: var(--radius-lg);
        color: var(--mat-sys-on-surface-variant);
        font-size: 0.8125rem;

        mat-icon {
          font-size: 1.25rem;
          width: var(--space-3);
          height: var(--space-3);
        }
      }
      mat-dialog-actions {
        padding: var(--space-4) var(--space-6);
        gap: var(--space-2);
      }
    `,
  ],
})
export class RoleDialog {
  roleForm: FormGroup;
  saving = signal(false);
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<RoleDialog>);
  public data: RoleDialogData = inject(MAT_DIALOG_DATA);
  private roleService = inject(RoleService);
  private snackBar = inject(MatSnackBar);
  private toastr = inject(ToastrService);
  private destroyRef = inject(DestroyRef);

  constructor() {
    // Get initial permission IDs for edit mode
    const initialPermissionIds = this.data.role?.permissions?.map((p) => p.id) || [];

    this.roleForm = this.fb.group({
      name: [this.data.role?.name || '', Validators.required],
      description: [this.data.role?.description || ''],
      permissions: [initialPermissionIds],
    });

    setupServerErrorClearing(this.roleForm, this.destroyRef);
  }

  onSubmit() {
    if (this.roleForm.valid) {
      const result: RoleRequest = {
        name: this.roleForm.value.name,
        description: this.roleForm.value.description,
        permissions: this.roleForm.value.permissions || [],
      };
      this.saving.set(true);

      if (this.data.mode === 'edit') {
        this.roleService.updateRoleById(this.data.role!.id, result).subscribe({
          next: () => {
            this.snackBar.open('Role updated', 'Close', { duration: 3000 });
            this.dialogRef.close(true);
          },
          error: (err) => {
            handleBackendFormErrors(err, this.roleForm, this.toastr);
            this.saving.set(false);
          },
        });
      } else {
        this.roleService.createRole(result).subscribe({
          next: () => {
            this.snackBar.open('Role created', 'Close', { duration: 3000 });
            this.dialogRef.close(true);
          },
          error: (err) => {
            handleBackendFormErrors(err, this.roleForm, this.toastr);
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
