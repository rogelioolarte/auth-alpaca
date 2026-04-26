import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltip } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { PermisionService } from '@app/api/permision-service';
import { Page, Pageable } from '@app/models/pageable';
import { Permission } from '@app/models/permission';
import {
  PermissionDialog,
  PermissionDialogData,
} from './dialogs/permission-dialog/permission-dialog';

@Component({
  selector: 'app-permissions',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIcon,
    MatTooltip,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    MatButtonModule,
  ],
  templateUrl: './permissions.html',
  styleUrl: './permissions.css',
})
export class Permissions {
  private permissionService = inject(PermisionService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  displayedColumns = [/* 'id',  */ 'name', 'actions'];
  permissions = signal<Permission[]>([]);
  totalItems = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);

  constructor() {
    this.loadPermissions();
  }

  loadPermissions() {
    const pageable: Pageable = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: ['id,desc'],
    };

    this.permissionService.getAllPagePermissions(pageable).subscribe({
      next: (page: Page<Permission>) => {
        this.permissions.set(page.content);
        this.totalItems.set(page.page.totalElements);
      },
      error: () => {
        this.snackBar.open('Error loading permissions', 'Close', { duration: 3000 });
      },
    });
  }

  applyFilter(event: Event) {
    const searchValue = (event.target as HTMLInputElement).value;
    // Implement filter logic here
    console.log('Filter:', searchValue);
  }

  onPageChange(event: PageEvent) {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
    this.loadPermissions();
  }

  openDialog(permission?: Permission) {
    const dialogData: PermissionDialogData = {
      mode: permission ? 'edit' : 'create',
      permission: permission ? { id: permission.id, name: permission.name } : undefined,
    };

    const dialogRef = this.dialog.open(PermissionDialog, {
      data: dialogData,
      width: '400px',
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        if (permission) {
          this.permissionService.updatePermissionById(permission.id, result.name).subscribe({
            next: () => {
              this.snackBar.open('Permission updated', 'Close', { duration: 3000 });
              this.loadPermissions();
            },
            error: () => {
              this.snackBar.open('Error updating permission', 'Close', { duration: 3000 });
            },
          });
        } else {
          this.permissionService.createPermission(result.name).subscribe({
            next: () => {
              this.snackBar.open('Permission created', 'Close', { duration: 3000 });
              this.loadPermissions();
            },
            error: () => {
              this.snackBar.open('Error creating permission', 'Close', { duration: 3000 });
            },
          });
        }
      }
    });
  }

  deletePermission(id: string) {
    if (confirm('Are you sure you want to delete this permission?')) {
      this.permissionService.deletePermissionById(id).subscribe({
        next: () => {
          this.snackBar.open('Permission deleted', 'Close', { duration: 3000 });
          this.loadPermissions();
        },
        error: () => {
          this.snackBar.open('Error deleting permission', 'Close', { duration: 3000 });
        },
      });
    }
  }
}
