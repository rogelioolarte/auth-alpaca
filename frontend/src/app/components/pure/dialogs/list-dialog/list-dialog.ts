import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { CommonModule } from '@angular/common';

export interface ListDialogData {
  title: string;
  items: { id: string; name: string }[];
  emptyMessage?: string;
}

@Component({
  selector: 'app-list-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatChipsModule],
  template: `
    <div class="list-dialog">
      <div class="dialog-header">
        <h2>{{ data.title }}</h2>
        <button mat-icon-button (click)="close()">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content>
        @if (data.items.length === 0) {
          <div class="empty-state">
            <mat-icon>inbox</mat-icon>
            <span>{{ data.emptyMessage || 'No items to display' }}</span>
          </div>
        } @else {
          <div class="items-list">
            @for (item of data.items; track item.id) {
              <mat-chip>{{ item.name }}</mat-chip>
            }
          </div>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="close()">Close</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .list-dialog {
        min-width: 350px;
        max-width: 500px;
      }
      .dialog-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 24px;
        border-bottom: 1px solid var(--mat-sys-outline-variant);

        h2 {
          margin: 0;
          font-size: 20px;
          font-weight: 500;
        }
      }
      mat-dialog-content {
        padding: 24px;
        min-height: 100px;
      }
      .items-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }
      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 12px;
        padding: 32px;
        color: var(--mat-sys-on-surface-variant);

        mat-icon {
          font-size: 48px;
          width: 48px;
          height: 48px;
          opacity: 0.5;
        }
      }
      mat-dialog-actions {
        padding: 16px 24px;
      }
    `,
  ],
})
export class ListDialog {
  constructor(
    private dialogRef: MatDialogRef<ListDialog>,
    @Inject(MAT_DIALOG_DATA) public data: ListDialogData,
  ) {}

  close() {
    this.dialogRef.close();
  }
}
