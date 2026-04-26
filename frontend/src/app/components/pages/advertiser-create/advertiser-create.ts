import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdvertiserService } from '@app/api/advertiser-service';
import { AuthenticationService } from '@app/auth/authentication-service';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-advertiser-create',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatProgressSpinner,
    MatFormFieldModule,
    MatInputModule,
    MatIcon,
    MatSlideToggleModule,
    MatButtonModule,
  ],
  templateUrl: './advertiser-create.html',
  styleUrl: './advertiser-create.css',
})
export class AdvertiserCreate {
  private fb = inject(FormBuilder);
  private advertiserService = inject(AdvertiserService);
  private authService = inject(AuthenticationService);
  private snackBar = inject(MatSnackBar);

  loading = signal(true);
  saving = signal(false);
  user = toSignal(this.authService.getUserInfo());

  advertiserForm: FormGroup = this.fb.group({
    id: [''],
    title: ['', Validators.required],
    description: [''],
    email: ['', [Validators.required, Validators.email]],
    publicLocation: [''],
    publicUrlLocation: [''],
    avatarUrl: [''],
    bannerUrl: [''],
    indexed: [false],
    paid: [false],
    verified: [false],
    userId: [''],
  });

  constructor() {
    this.loadAdvertiser();
  }

  loadAdvertiser() {
    const userInfo = this.user();
    if (userInfo?.advertiserId) {
      this.advertiserService.getAdvertiserById(userInfo.advertiserId).subscribe({
        next: (advertiser) => {
          this.advertiserForm.patchValue(advertiser);
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Error loading advertiser', 'Close', { duration: 3000 });
          this.loading.set(false);
        },
      });
    } else {
      this.loading.set(false);
    }
  }

  onSubmit() {
    if (this.advertiserForm.valid) {
      this.saving.set(true);
      const userId = this.user()?.id || '';
      const advertiserData = this.advertiserForm.value;
      advertiserData.userId = userId;

      if (advertiserData.id) {
        this.advertiserService.updateAdvertiserById(advertiserData.id, advertiserData).subscribe({
          next: () => {
            this.snackBar.open('Advertiser updated successfully', 'Close', { duration: 3000 });
            this.saving.set(false);
          },
          error: () => {
            this.snackBar.open('Error updating advertiser', 'Close', { duration: 3000 });
            this.saving.set(false);
          },
        });
      } else {
        this.advertiserService.createAdvertiser(advertiserData).subscribe({
          next: () => {
            this.snackBar.open('Advertiser created successfully', 'Close', { duration: 3000 });
            this.saving.set(false);
            this.authService.rotateTokens().subscribe();
          },
          error: () => {
            this.snackBar.open('Error creating advertiser', 'Close', { duration: 3000 });
            this.saving.set(false);
          },
        });
      }
    }
  }

  openPublicUrl() {
    const url = this.advertiserForm.get('publicUrlLocation')?.value;
    if (url) {
      window.open(url, '_blank');
    }
  }

  openAvatarUrl() {
    const url = this.advertiserForm.get('avatarUrl')?.value;
    if (url) {
      window.open(url, '_blank');
    }
  }

  openBannerUrl() {
    const url = this.advertiserForm.get('bannerUrl')?.value;
    if (url) {
      window.open(url, '_blank');
    }
  }
}
