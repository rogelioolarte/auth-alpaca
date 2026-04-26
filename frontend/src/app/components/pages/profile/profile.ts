import { Component, inject, signal } from '@angular/core';
import { AuthenticationService } from '../../../auth/authentication-service';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatIcon } from '@angular/material/icon';
import { ProfileService } from '@app/api/profile-service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatInputModule } from '@angular/material/input';
import { Profile } from '@app/models/profile';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-profile',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatProgressSpinner,
    MatFormFieldModule,
    MatInputModule,
    MatIcon,
    MatButtonModule,
  ],
  templateUrl: `./profile.html`,
  styleUrl: `./profile.css`,
})
export class ProfileCreate {
  private readonly fb = inject(FormBuilder);
  private readonly profileService = inject(ProfileService);
  private readonly authService = inject(AuthenticationService);
  private readonly snackBar = inject(MatSnackBar);

  public loading = signal(true);
  public saving = signal(false);
  public readonly profile = toSignal(this.authService.getUserInfo());

  profileForm: FormGroup = this.fb.group({
    id: [''],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    address: [''],
    avatarUrl: [''],
    userId: [''],
  });

  constructor() {
    this.loadProfile();
  }

  loadProfile() {
    const userInfo = this.profile();
    if (userInfo?.profileId) {
      this.profileService.getProfileById(userInfo.profileId).subscribe({
        next: (profile) => {
          this.profileForm.patchValue(profile);
          this.loading.set(false);
        },
        error: () => {
          this.snackBar.open('Error loading profile', 'Close', { duration: 3000 });
          this.loading.set(false);
        },
      });
    } else {
      this.loading.set(false);
    }
  }

  onSubmit() {
    if (this.profileForm.valid) {
      this.saving.set(true);
      const profileData: Profile = this.profileForm.value;

      if (profileData.id) {
        this.profileService.updateProfileById(profileData.id, profileData).subscribe({
          next: () => {
            this.snackBar.open('Profile updated successfully', 'Close', { duration: 3000 });
            this.saving.set(false);
          },
          error: () => {
            this.snackBar.open('Error updating profile', 'Close', { duration: 3000 });
            this.saving.set(false);
          },
        });
      } else {
        this.profileService.createProfile(profileData).subscribe({
          next: () => {
            this.snackBar.open('Profile created successfully', 'Close', { duration: 3000 });
            this.saving.set(false);
            this.authService.rotateTokens().subscribe();
          },
          error: () => {
            this.snackBar.open('Error creating profile', 'Close', { duration: 3000 });
            this.saving.set(false);
          },
        });
      }
    }
  }

  openAvatarUrl() {
    const url = this.profileForm.get('avatarUrl')?.value;
    if (url) {
      window.open(url, '_blank');
    }
  }
}
