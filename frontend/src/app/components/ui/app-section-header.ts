import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-section-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="mb-4 flex items-center justify-between gap-4">
      <h2 class="text-[10px] font-bold uppercase tracking-[0.08em] text-[var(--brand-muted)]">{{ label }}</h2>
      @if (actionLabel && actionRoute) {
        <a [routerLink]="actionRoute" class="text-xs font-semibold text-[var(--brand-blue)] transition hover:text-cyan-300">
          {{ actionLabel }}
        </a>
      }
    </div>
  `,
})
export class AppSectionHeaderComponent {
  @Input({ required: true }) label!: string;
  @Input() actionLabel?: string;
  @Input() actionRoute?: string;
}
