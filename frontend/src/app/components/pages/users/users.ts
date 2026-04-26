import { Component, inject, OnInit, signal } from '@angular/core';
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
import { UserService } from '@app/api/user-service';
import { RoleService } from '@app/api/role-service';
import { Page, Pageable } from '@app/models/pageable';
import { User } from '@app/models/user';
import { Role } from '@app/models/role';
import { UserDialog, UserDialogData } from './dialogs/user-dialog/user-dialog';
import { ListDialog, ListDialogData } from '@app/components/pure/dialogs/list-dialog/list-dialog';

@Component({
  selector: 'app-users',
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
  templateUrl: './users.html',
  styleUrl: './users.css',
})
export class Users implements OnInit {
  private readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  public displayedColumns = [/* 'id',  */ 'email', 'profile', 'advertiser', 'roles', 'actions'];
  public users = signal<User[]>([]);
  public roles = signal<Role[]>([]);
  public totalItems = signal(0);
  public pageSize = signal(10);
  public pageIndex = signal(0);

  ngOnInit() {
    this.loadUsers();
    this.loadRoles();
  }

  loadRoles() {
    this.roleService.getAllRoles().subscribe({
      next: (roles) => this.roles.set(roles),
      error: () => this.snackBar.open('Error loading roles', 'Close', { duration: 3000 }),
    });
  }

  loadUsers() {
    const pageable: Pageable = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: ['id,desc'],
    };

    this.userService.getAllPageUsers(pageable).subscribe({
      next: (page: Page<User>) => {
        this.users.set(page.content);
        this.totalItems.set(page.page.totalElements);
      },
      error: () => {
        this.snackBar.open('Error loading users', 'Close', { duration: 3000 });
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
    this.loadUsers();
  }

  openDialog(user?: User) {
    const dialogData: UserDialogData = {
      mode: user ? 'edit' : 'create',
      user: user ? { id: user.id, email: user.email, roles: user.roles } : undefined,
      availableRoles: this.roles(),
    };

    const dialogRef = this.dialog.open(UserDialog, {
      data: dialogData,
      width: '500px',
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        if (user) {
          // Edit mode - update user
          this.userService.updateUserById(user.id, result).subscribe({
            next: () => {
              this.snackBar.open('User updated', 'Close', { duration: 3000 });
              this.loadUsers();
            },
            error: () => {
              this.snackBar.open('Error updating user', 'Close', { duration: 3000 });
            },
          });
        } else {
          // Create mode
          this.userService.createUser(result).subscribe({
            next: () => {
              this.snackBar.open('User created', 'Close', { duration: 3000 });
              this.loadUsers();
            },
            error: () => {
              this.snackBar.open('Error creating user', 'Close', { duration: 3000 });
            },
          });
        }
      }
    });
  }

  deleteUser(id: string) {
    if (confirm('Are you sure you want to delete this user?')) {
      this.userService.deleteUserById(id).subscribe({
        next: () => {
          this.snackBar.open('User deleted', 'Close', { duration: 3000 });
          this.loadUsers();
        },
        error: () => {
          this.snackBar.open('Error deleting user', 'Close', { duration: 3000 });
        },
      });
    }
  }

  openRolesDialog(user: User) {
    const dialogData: ListDialogData = {
      title: `Roles - ${user.email}`,
      items: user.roles || [],
      emptyMessage: 'No roles assigned',
    };

    this.dialog.open(ListDialog, {
      data: dialogData,
      width: '450px',
    });
  }
}
