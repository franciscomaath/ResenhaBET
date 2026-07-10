import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-match-list-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './match-list-section.html',
})
export class MatchListSectionComponent {
  @Input({ required: true }) title!: string;
  @Input() subtitle?: string;
  @Input() sticky = false;
  @Input() compact = false;
}
