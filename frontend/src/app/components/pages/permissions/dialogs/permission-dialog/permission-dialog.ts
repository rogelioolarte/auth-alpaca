import { Component, inject, signal, DestroyRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PermisionService } from '@app/api/permision-service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ToastrService } from 'ngx-toastr';
import { handleBackendFormErrors, setupServerErrorClearing } from '@app/shared/utils/form-error-handler.util';

export interface PermissionDialogData {
  mode: 'create' | 'edit';
  permission?: {
    id: string;
    name: string;
  };
}

@Component({
  selector: 'app-permission-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <h2>{{ data.mode === 'create' ? 'Create Permission' : 'Edit Permission' }}</h2>
        <button mat-icon-button (click)="onCancel()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <form [formGroup]="permissionForm" (ngSubmit)="onSubmit()">
        <mat-dialog-content>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" placeholder="e.g., READ_USERS" />
            @if (permissionForm.get('name')?.hasError('required')) {
              <mat-error>Name is required</mat-error>
            }
            @if (permissionForm.get('name')?.hasError('serverError')) {
              <mat-error>{{ permissionForm.get('name')?.getError('serverError') }}</mat-error>
            }
          </mat-form-field>
        </mat-dialog-content>

        <mat-dialog-actions align="end">
          <button mat-button type="button" (click)="onCancel()">Cancel</button>
          <button
            mat-flat-button
            color="primary"
            type="submit"
            [disabled]="permissionForm.invalid || saving()"
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
        min-width: 25rem;
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
      }
      mat-dialog-actions {
        padding: var(--space-4) var(--space-6);
        gap: var(--space-2);
      }
    `,
  ],
})
export class PermissionDialog {
  permissionForm: FormGroup;
  saving = signal(false);
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<PermissionDialog>);
  public data: PermissionDialogData = inject(MAT_DIALOG_DATA);
  private permissionService = inject(PermisionService);
  private snackBar = inject(MatSnackBar);
  private toastr = inject(ToastrService);
  private destroyRef = inject(DestroyRef);

  constructor() {
    this.permissionForm = this.fb.group({
      name: [this.data.permission?.name || '', Validators.required],
    });

    setupServerErrorClearing(this.permissionForm, this.destroyRef);
  }

  onSubmit() {
    if (this.permissionForm.valid) {
      const name = this.permissionForm.value.name;
      this.saving.set(true);

      if (this.data.mode === 'edit') {
        this.permissionService.updatePermissionById(this.data.permission!.id, name).subscribe({
          next: () => {
            this.snackBar.open('Permission updated', 'Close', { duration: 3000 });
            this.dialogRef.close(true);
          },
          error: (err) => {
            handleBackendFormErrors(err, this.permissionForm, this.toastr);
            this.saving.set(false);
          },
        });
      } else {
        this.permissionService.createPermission(name).subscribe({
          next: () => {
            this.snackBar.open('Permission created', 'Close', { duration: 3000 });
            this.dialogRef.close(true);
          },
          error: (err) => {
            handleBackendFormErrors(err, this.permissionForm, this.toastr);
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
