import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SessionService } from '../../../api/session-service';
import { Pageable } from '../../../models/pageable';
import { Session } from '@app/models/session';
import { ConfirmDialog } from './dialog/confirm-dialog';
import { AuthenticationService } from '@app/auth/authentication-service';
import { Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-manage-sessions',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatTooltipModule,
    MatIconModule,
    MatProgressBarModule,
    MatButtonModule,
    MatDialogModule,
  ],
  templateUrl: './manage-sessions.html',
  styleUrls: ['./manage-sessions.css'],
})
export class ManageSessions implements OnInit {
  private readonly sessionService = inject(SessionService);
  private readonly authenticationService = inject(AuthenticationService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private clientId = toSignal(this.authenticationService.getClientID());

  public displayedColumns = ['ipAddress', 'userAgent', 'lastSeenAt', 'actions'];
  public sessions = signal<Session[]>([]);
  public totalItems = signal(0);
  public pageSize = signal(10);
  public pageIndex = signal(0);
  public isLoading = signal(false);

  ngOnInit(): void {
    this.loadSessions();
  }

  public loadSessions(): void {
    this.isLoading.set(true);
    const pageable: Pageable = {
      page: this.pageIndex(),
      size: this.pageSize(),
    };

    this.sessionService.getSessions(pageable).subscribe({
      next: (page) => {
        this.sessions.set(page.content);
        this.totalItems.set(page.page.totalElements);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  public onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadSessions();
  }

  public truncateString(value: string): string {
    if (!value) return '';
    if (value.length <= 20) return value;
    return value.substring(0, 20) + '...';
  }

  public revokeSession(id: string): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Revoke Session',
        message: 'Are you sure you want to revoke this session?',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.sessionService.revokeSession(id).subscribe({
          next: () => {
            this.snackBar.open('Session revoked successfully', 'Close', { duration: 3000 });
            this.loadSessions();
            const mySession = this.sessions().find(s => s.id === id);
            if(mySession?.clientId === this.clientId()) {
              this.revokeMySession();
            }
          },
          error: (err) => {
            this.snackBar.open(`Error revoking session: ${err.message || 'Unknown error'}`, 'Close', { duration: 3000 });
          },
        });
      }
    });
  }

  public revokeAllOtherSessions(): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: {
        title: 'Revoke All Other Sessions',
        message: 'Are you sure you want to revoke all other active sessions? Your current session will be closed.',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.sessionService.revokeAllSessions().subscribe({
          next: () => {
            this.snackBar.open('All sessions revoked successfully', 'Close', { duration: 3000 });
            this.revokeMySession();
          },
          error: (err) => {
            this.snackBar.open(`Error revoking sessions: ${err.message || 'Unknown error'}`, 'Close', { duration: 3000 });
          },
        });
        
      }
    });
  }

  private revokeMySession() {
    this.authenticationService.cleanStates();
    this.router.navigateByUrl("/login");
  }
}
