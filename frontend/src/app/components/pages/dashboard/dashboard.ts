import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { AuthenticationService } from '../../../auth/authentication-service';
import { Router, RouterOutlet } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { MatIcon } from '@angular/material/icon';
import {MatToolbar} from "@angular/material/toolbar";
import {MatIconButton} from "@angular/material/button";
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-dashboard',
  imports: [RouterOutlet, MatToolbar, MatIcon, MatIconButton],
  template: `
  <div>
    @if(isAuthenticated()) {
      <mat-toolbar color="primary">
        <span class="header">User Console</span>
        <span class="example-spacer"></span>
        <button
          mat-icon-button
          class="example-icon favorite-icon"
          (click)="logout()">
          <mat-icon class="mat-icon-large">logout</mat-icon>
        </button>
      </mat-toolbar>
    }
  </div>
  <router-outlet></router-outlet>
  `,
  styles: ``,
})
export class Dashboard implements OnInit, OnDestroy {
  private authService = inject(AuthenticationService)
  public readonly isAuthenticated = toSignal(this.authService.isAuthenticated())
  private destroy = new Subject<void>()
  private readonly router = inject(Router)

  ngOnInit() {
    this.authService.setUserInfo().pipe(takeUntil(this.destroy)).subscribe()
  }

  logout() {
    this.authService.logout().subscribe(() => this.router.navigateByUrl('/login'))
  }

  ngOnDestroy() {
    this.destroy.next()
    this.destroy.complete()
  }
}