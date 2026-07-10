import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-amount-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span [ngClass]="amountClasses">
      {{ prefix }}{{ value }}{{ suffix }}
    </span>
  `,
})
export class AmountBadgeComponent {
  @Input({ required: true }) value!: string | number;
  @Input() prefix = '';
  @Input() suffix = '';
  @Input() tone: 'positive' | 'negative' | 'neutral' = 'neutral';

  get amountClasses(): string {
    const base = 'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-bold tabular-nums';
    const tones: Record<typeof this.tone, string> = {
      positive: 'bg-green-950/40 text-green-200',
      negative: 'bg-red-950/40 text-red-200',
      neutral: 'bg-[var(--surface-1)] text-[var(--brand-text)]',
    };

    return `${base} ${tones[this.tone]}`;
  }
}
