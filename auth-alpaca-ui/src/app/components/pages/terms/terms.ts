import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-terms',
  imports: [RouterLink],
  templateUrl: './terms.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './terms.css',
})
export class Terms {}
