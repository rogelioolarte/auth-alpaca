import { Component, inject, OnInit, signal, DestroyRef } from '@angular/core';
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
import { AdvertiserRequest } from '@app/models/advertiser';
import { ToastrService } from 'ngx-toastr';
import { handleBackendFormErrors, setupServerErrorClearing } from '@app/shared/utils/form-error-handler.util';

@Component({
  selector: 'app-my-advertiser',
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
  templateUrl: './my-advertiser.html',
  styleUrl: './my-advertiser.css',
})
export class MyAdvertiser implements OnInit {
  private fb = inject(FormBuilder);
  private advertiserService = inject(AdvertiserService);
  private authService = inject(AuthenticationService);
  private snackBar = inject(MatSnackBar);
  private toastr = inject(ToastrService);
  private destroyRef = inject(DestroyRef);

  loading = signal(true);
  saving = signal(false);
  user = toSignal(this.authService.getUserInfo());

  advertiserForm: FormGroup = this.fb.group({
    title: ['', Validators.required],
    description: [''],
    publicLocation: [''],
    publicUrlLocation: [''],
    avatarUrl: [''],
    bannerUrl: [''],
    indexed: [false],
  });

  ngOnInit(): void {
    this.loadAdvertiser();
    setupServerErrorClearing(this.advertiserForm, this.destroyRef);
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
      const advertiserData: AdvertiserRequest = this.advertiserForm.value;
      advertiserData.id = this.user()?.advertiserId;
      advertiserData.userId = userId;

      if (advertiserData.id) {
        this.advertiserService.updateAdvertiserById(advertiserData.id, advertiserData).subscribe({
          next: () => {
            this.snackBar.open('Advertiser updated successfully', 'Close', { duration: 3000 });
            this.saving.set(false);
          },
          error: (err) => {
            handleBackendFormErrors(err, this.advertiserForm, this.toastr);
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
          error: (err) => {
            handleBackendFormErrors(err, this.advertiserForm, this.toastr);
            this.snackBar.open('Error creating advertiser', 'Close', { duration: 3000 });
            this.saving.set(false);
          },
        });
      }
    }
  }

  openExternalURL(key: string) {
    const url: string = this.advertiserForm.get(key)?.value;
    if (url) {
      window.open(url, '_blank');
    }
  }
}
