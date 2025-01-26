import {Component, OnInit} from '@angular/core';
import {MatToolbar} from "@angular/material/toolbar";
import {MatIcon} from "@angular/material/icon";
import {RouterOutlet} from "@angular/router";
import {ApiService} from "../../api/api.service";
import {AuthService} from "../../auth/auth.service";
import {catchError, EMPTY, tap} from "rxjs";
import {response} from "express";
import {MatIconButton} from "@angular/material/button";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    MatToolbar,
    MatIcon,
    RouterOutlet,
    MatIconButton,
    NgIf
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  constructor(
    private apiService: ApiService,
    public authService: AuthService
  ) {
  }

  ngOnInit() {
    this.loadCurrentlyLoggedInUser();
  }

  loadCurrentlyLoggedInUser() {
    this.apiService.getCurrentUser()
      .pipe(
        tap(response => {
          this.authService.authenticated = true;
        }),
        catchError(err => {
          return EMPTY;
        })
      )
  }

}
