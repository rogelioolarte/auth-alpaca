import {Component, OnInit} from '@angular/core';
import {AuthProvider} from "../../model/model";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {ToastrService} from "ngx-toastr";
import {AuthService} from "../../auth/auth.service";
import {GITHUB_AUTH_URL, GOOGLE_AUTH_URL} from "../../model/constants";
import {ApiService} from "../../api/api.service";
import {MatFormField, MatLabel} from "@angular/material/form-field";
import {MatCard, MatCardContent, MatCardHeader, MatCardTitle} from "@angular/material/card";
import {MatIcon} from "@angular/material/icon";
import {MatButton, MatIconButton} from "@angular/material/button";
import {MatInput} from "@angular/material/input";

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormField,
    MatCard,
    MatCardHeader,
    MatCardContent,
    MatIcon,
    MatIconButton,
    MatInput,
    MatButton,
    MatFormField,
    MatLabel,
    MatCardTitle
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  authProvider?: AuthProvider;
  loginForm!: FormGroup;
  loading = false;
  submitted = false;

  errorResponse: boolean = false;
  errorMessage: string = '';

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private toastrService: ToastrService,
    private authService: AuthService,
    private apiService: ApiService
  ) {
  }

  ngOnInit() {
    this.loginForm = this.formBuilder.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  onSubmit() {
    this.errorResponse = false;
    this.submitted = true;

    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.apiService.login(this.loginForm.value)
      .subscribe({
        next: data => {
          const token = JSON.parse(JSON.stringify(data)).token;
          if (token) {
            this.authProvider = AuthProvider.local;
            this.authService.setAuthentication(token);
            this.router.navigate(['/dashboard/profile', this.authProvider],
              {state: {from: this.router.routerState.snapshot.url}});
          } else {
            this.errorResponse = true;
            this.errorMessage = "Authentication failed.";
          }
        },
        error: error => {
          this.errorResponse = true;
          this.errorMessage = error.error.message;
          this.loading = false;
        }
      });
  }

  loginWithProvider(provider: AuthProvider) {
    switch (provider) {
      case AuthProvider.google:
        window.location.href = GOOGLE_AUTH_URL;
        break;
      case AuthProvider.github:
        window.location.href = GITHUB_AUTH_URL;
        break;
      default:
        this.toastrService.error('Unknown provider');
    }
  }

  protected readonly AuthProvider = AuthProvider;

}
