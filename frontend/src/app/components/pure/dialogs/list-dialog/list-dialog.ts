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
        container-type: inline-size;

        .dialog-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: var(--space-4) var(--space-6);
          border-bottom: 1px solid var(--color-outline-variant);

          h2 {
            margin: 0;
            font-size: 1.25rem;
            font-weight: 500;
          }
        }

        mat-dialog-content {
          padding: var(--space-6);
          min-height: 100px;
        }

        .items-list {
          display: flex;
          flex-wrap: wrap;
          gap: var(--space-2);
        }

        .empty-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--space-3);
          padding: var(--space-8);
          color: var(--color-on-surface-variant);

          mat-icon {
            font-size: 3rem;
            width: 3rem;
            height: 3rem;
            opacity: 0.5;
          }
        }

        mat-dialog-actions {
          padding: var(--space-4) var(--space-6);
        }

        @container (max-width: 400px) {
          .dialog-header {
            flex-direction: column;
            align-items: flex-start;
            gap: var(--space-2);
          }
        }
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
