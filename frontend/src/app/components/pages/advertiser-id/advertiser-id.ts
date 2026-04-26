import { Component, inject, OnInit, signal } from '@angular/core';
import { MatCard, MatCardActions, MatCardContent } from '@angular/material/card';
import { MatChip } from '@angular/material/chips';
import { MatDivider } from '@angular/material/divider';
import { MatIcon } from '@angular/material/icon';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AdvertiserService } from '@app/api/advertiser-service';
import { Advertiser } from '@app/models/advertiser';

@Component({
  selector: 'app-advertiser-id',
  imports: [
    RouterLink,
    MatProgressSpinner,
    MatIcon,
    MatChip,
    MatCard,
    MatCardContent,
    MatDivider,
    MatCardActions,
  ],
  templateUrl: './advertiser-id.html',
  styleUrl: './advertiser-id.css',
})
export class AdvertiserId implements OnInit {
  private route = inject(ActivatedRoute);
  private advertiserService = inject(AdvertiserService);

  loading = signal(true);
  advertiser = signal<Advertiser | null>(null);

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadAdvertiser(id);
    } else {
      this.loading.set(false);
    }
  }

  loadAdvertiser(id: string) {
    this.loading.set(true);
    this.advertiserService.getAdvertiserById(id).subscribe({
      next: (advertiser: Advertiser) => {
        this.advertiser.set(advertiser);
        this.loading.set(false);
      },
      error: () => {
        this.advertiser.set(null);
        this.loading.set(false);
      },
    });
  }
}
