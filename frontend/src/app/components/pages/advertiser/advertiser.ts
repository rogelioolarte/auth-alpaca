import { Component, inject, signal } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatChip } from '@angular/material/chips';
import { MatFormField, MatFormFieldModule, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { AdvertiserService } from '@app/api/advertiser-service';
import { Pageable } from '@app/models/pageable';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { Advertiser } from '@app/models/advertiser';
import { SlicePipe } from '@angular/common';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-advertiser',
  imports: [
    MatFormField,
    MatLabel,
    MatProgressSpinner,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIcon,
    MatCard,
    ReactiveFormsModule,
    RouterLink,
    MatCardContent,
    MatChip,
    MatPaginator,
    FormsModule,
    SlicePipe,
  ],
  templateUrl: './advertiser.html',
  styleUrl: './advertiser.css',
})
export class AdvertiserPage {
  private advertiserService = inject(AdvertiserService);
  private snackBar = inject(MatSnackBar);
  private searchSubject = new Subject<string>();

  loading = signal(true);
  advertisers = signal<Advertiser[]>([]);
  totalItems = signal(0);
  pageSize = signal(12);
  pageIndex = signal(0);
  searchQuery = '';

  constructor() {
    this.loadAdvertisers();

    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.loadAdvertisers();
    });
  }

  loadAdvertisers() {
    this.loading.set(true);
    const pageable: Pageable = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: ['title,asc'],
    };

    this.advertiserService.getAllPageAdvertisers(pageable).subscribe({
      next: (page) => {
        this.advertisers.set(page.content);
        this.totalItems.set(page.page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Error loading advertisers', 'Close', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  onSearchChange(query: string) {
    this.searchSubject.next(query);
  }

  onPageChange(event: PageEvent) {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);
    this.loadAdvertisers();
  }
}
