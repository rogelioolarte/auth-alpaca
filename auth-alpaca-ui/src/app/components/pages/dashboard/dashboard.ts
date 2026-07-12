import { Component, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthenticationService } from '@app/auth/authentication-service';
import { UserService } from '@app/api/user-service';
import { User } from '@app/models/user';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatIcon,
    MatButtonModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthenticationService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  public readonly userAuth = toSignal(this.authService.getUserInfo());
  public user = signal<User | null>(null);
  public loading = signal(true);
  public error = signal<string | null>(null);

  ngOnInit() {
    this.loadUser();
  }

  loadUser() {
    const userId = this.userAuth()?.id;
    if (!userId) {
      this.error.set('User not authenticated');
      this.loading.set(false);
      return;
    }

    this.userService.getUserById(userId).subscribe({
      next: (user) => {
        this.user.set(user);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading user:', err);
        this.error.set('Failed to load user data');
        this.loading.set(false);
      }
    });
  }

  getUserInitials(): string {
    const profile = this.user()?.profile;
    if (profile?.firstName && profile?.lastName) {
      return `${profile.firstName[0]}${profile.lastName[0]}`.toUpperCase();
    }
    const username = this.userAuth()?.username || '';
    return username.slice(0, 2).toUpperCase();
  }

  getFullName(): string {
    const profile = this.user()?.profile;
    if (profile?.firstName && profile?.lastName) {
      return `${profile.firstName} ${profile.lastName}`;
    }
    return this.userAuth()?.username || 'User';
  }

  getRoleDisplayName(role: string): string {
    return role.replace('ROLE_', '');
  }
}