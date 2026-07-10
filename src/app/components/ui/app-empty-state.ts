import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col items-center justify-center rounded-[var(--radius-lg)] border border-dashed border-[var(--brand-border)] bg-[var(--surface-2)]/50 p-8 text-center">
      @if (icon) {
        <span class="material-icons mb-4 text-4xl text-[var(--brand-muted)]">{{ icon }}</span>
      }
      <h3 class="mb-2 text-lg font-bold text-[var(--brand-text)]">{{ title }}</h3>
      @if (subtitle) {
        <p class="mb-6 max-w-sm text-sm text-[var(--brand-muted)]">{{ subtitle }}</p>
      }
      <ng-content></ng-content>
    </div>
  `,
})
export class AppEmptyStateComponent {
  @Input() icon?: string;
  @Input({ required: true }) title!: string;
  @Input() subtitle?: string;
}
