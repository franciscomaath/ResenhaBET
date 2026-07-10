import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span [ngClass]="badgeClasses">
      {{ displayLabel }}
    </span>
  `,
})
export class StatusBadgeComponent {
  @Input() variant:
    | 'live'
    | 'open'
    | 'suspended'
    | 'closed'
    | 'pending'
    | 'won'
    | 'lost'
    | 'created'
    | 'in-progress'
    | 'completed'
    | 'danger'
    | 'neutral' = 'neutral';
  @Input() label?: string;

  get displayLabel(): string {
    return this.label ?? this.variant.replaceAll('-', ' ');
  }

  get badgeClasses(): string {
    const base = 'inline-flex items-center gap-2 rounded-full px-3 py-1 text-[11px] font-bold uppercase tracking-[0.5px]';
    const variants: Record<typeof this.variant, string> = {
      live: 'border border-red-500/20 bg-red-950/40 text-red-200',
      open: 'border border-green-500/20 bg-green-950/40 text-green-200',
      suspended: 'border border-yellow-500/20 bg-yellow-950/40 text-yellow-200',
      closed: 'border border-[var(--brand-border)] bg-[var(--surface-1)] text-[var(--brand-muted)]',
      pending: 'border border-[var(--brand-border)] bg-[var(--surface-1)] text-gray-300',
      won: 'border border-green-500/20 bg-green-950/40 text-green-200',
      lost: 'border border-red-500/20 bg-red-950/40 text-red-200',
      created: 'border border-[var(--brand-border)] bg-[var(--surface-1)] text-gray-300',
      'in-progress': 'border border-green-500/20 bg-green-950/40 text-green-200',
      completed: 'border border-[var(--brand-border)] bg-[var(--surface-1)] text-gray-300',
      danger: 'border border-red-500/20 bg-red-950/40 text-red-200',
      neutral: 'border border-[var(--brand-border)] bg-[var(--surface-1)] text-[var(--brand-muted)]',
    };

    return `${base} ${variants[this.variant]}`;
  }
}
