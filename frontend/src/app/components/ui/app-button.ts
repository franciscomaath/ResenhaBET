import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button [attr.type]="type" [disabled]="disabled" [ngClass]="buttonClasses">
      <ng-content></ng-content>
    </button>
  `,
})
export class AppButtonComponent {
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() variant: 'primary' | 'secondary' | 'ghost' | 'danger' | 'admin-inline' | 'accent' = 'primary';
  @Input() disabled = false;
  @Input() fullWidth = false;
  @Input() compact = false;

  get buttonClasses(): string {
    const base = 'inline-flex items-center justify-center gap-2 rounded-[var(--radius-md)] font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50';
    const width = this.fullWidth ? 'w-full' : '';
    const size = this.compact ? 'min-h-9 px-3 py-2 text-sm' : 'min-h-11 px-4 py-3 text-sm';

    const variants: Record<typeof this.variant, string> = {
      primary: 'bg-[var(--brand-green)] text-[#06140d] hover:bg-green-500',
      secondary: 'border border-[var(--brand-border)] bg-[var(--surface-2)] text-white hover:bg-[var(--surface-1)]',
      ghost: 'bg-transparent text-[var(--brand-blue)] hover:bg-[rgba(61,139,253,0.1)]',
      danger: 'bg-red-800 text-white hover:bg-red-700',
      'admin-inline': 'border border-transparent bg-transparent px-2 py-1 text-xs text-[var(--brand-muted)] hover:border-[var(--brand-border)] hover:text-white',
      accent: 'bg-[var(--brand-blue)] text-white hover:bg-blue-500',
    };

    return [base, width, size, variants[this.variant]].filter(Boolean).join(' ');
  }
}
