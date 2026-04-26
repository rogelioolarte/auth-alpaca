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
import { RoleService } from '@app/api/role-service';
import { PermisionService } from '@app/api/permision-service';
import { Page, Pageable } from '@app/models/pageable';
import { Role } from '@app/models/role';
import { Permission } from '@app/models/permission';
import { RoleDialog, RoleDialogData } from './dialogs/role-dialog/role-dialog';
import { ListDialog, ListDialogData } from '@app/components/pure/dialogs/list-dialog/list-dialog';

@Component({
  selector: 'app-roles',
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
  templateUrl: './roles.html',
  styleUrl: './roles.css',
})
export class Roles {
  private readonly roleService = inject(RoleService);
  private readonly permissionService = inject(PermisionService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  displayedColumns = [/* 'id',  */ 'name', 'description', 'permissions', 'actions'];
  roles = signal<Role[]>([]);
  permissions = signal<Permission[]>([]);
  totalItems = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);

  constructor() {
    this.loadRoles();
    this.loadPermissions();
  }

  loadPermissions() {
    this.permissionService.getAllPermissions().subscribe({
      next: (permissions) => this.permissions.set(permissions),
      error: () => this.snackBar.open('Error loading permissions', 'Close', { duration: 3000 }),
    });
  }

  loadRoles() {
    const pageable: Pageable = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: ['id,desc'],
    };

    this.roleService.getAllPageRoles(pageable).subscribe({
      next: (page: Page<Role>) => {
        this.roles.set(page.content);
        this.totalItems.set(page.page.totalElements);
      },
      error: () => {
        this.snackBar.open('Error loading roles', 'Close', { duration: 3000 });
      },
    });
  }

  applyFilter(event: Event) {
    const searchValue = (event.target as HTMLInputElement).value;
    console.log('Filter:', searchValue);
  }

  onPageChange(event: PageEvent) {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
    this.loadRoles();
  }

  openDialog(role?: Role) {
    const dialogData: RoleDialogData = {
      mode: role ? 'edit' : 'create',
      role: role
        ? {
            id: role.id,
            name: role.name,
            description: role.description || '',
            permissions: role.permissions,
          }
        : undefined,
      availablePermissions: this.permissions(),
    };

    const dialogRef = this.dialog.open(RoleDialog, {
      data: dialogData,
      width: '500px',
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        if (role) {
          // Edit mode
          this.roleService.updateRoleById(role.id, result).subscribe({
            next: () => {
              this.snackBar.open('Role updated', 'Close', { duration: 3000 });
              this.loadRoles();
            },
            error: () => {
              this.snackBar.open('Error updating role', 'Close', { duration: 3000 });
            },
          });
        } else {
          // Create mode
          this.roleService.createRole(result).subscribe({
            next: () => {
              this.snackBar.open('Role created', 'Close', { duration: 3000 });
              this.loadRoles();
            },
            error: () => {
              this.snackBar.open('Error creating role', 'Close', { duration: 3000 });
            },
          });
        }
      }
    });
  }

  deleteRole(id: string) {
    if (confirm('Are you sure you want to delete this role?')) {
      this.roleService.deleteRoleById(id).subscribe({
        next: () => {
          this.snackBar.open('Role deleted', 'Close', { duration: 3000 });
          this.loadRoles();
        },
        error: () => {
          this.snackBar.open('Error deleting role', 'Close', { duration: 3000 });
        },
      });
    }
  }

  openPermissionsDialog(role: Role) {
    const dialogData: ListDialogData = {
      title: `Permissions - ${role.name}`,
      items: role.permissions || [],
      emptyMessage: 'No permissions assigned',
    };

    this.dialog.open(ListDialog, {
      data: dialogData,
      width: '450px',
    });
  }
}
