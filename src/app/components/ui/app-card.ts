import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article [ngClass]="cardClasses">
      <ng-content></ng-content>
    </article>
  `,
})
export class AppCardComponent {
  @Input() elevated = false;
  @Input() variant: 'surface' | 'subtle' | 'borderless' = 'surface';
  @Input() padded = true;

  get cardClasses(): string {
    const base = 'rounded-[var(--radius-lg)] border transition-colors';
    const padding = this.padded ? 'p-4 sm:p-6' : '';
    const variants: Record<typeof this.variant, string> = {
      surface: 'border-[var(--brand-border)] bg-[var(--surface-2)] shadow-brand-card',
      subtle: 'border-[var(--brand-border)] bg-[var(--surface-1)]',
      borderless: 'border-transparent bg-[var(--surface-2)]',
    };

    const elevated = this.elevated ? 'border-white/10 bg-[var(--surface-2)] shadow-brand-card' : '';

    return [base, padding, variants[this.variant], elevated].filter(Boolean).join(' ');
  }
}
