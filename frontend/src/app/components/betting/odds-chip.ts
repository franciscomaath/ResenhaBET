import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface OddsChipData {
  id: number;
  label: string;
  odd: number | string;
  selected?: boolean;
  disabled?: boolean;
  compact?: boolean;
  subLabel?: string;
}

@Component({
  selector: 'app-odds-chip',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      type="button"
      [ngClass]="chipClasses"
      [disabled]="disabled"
      (click)="clicked.emit(data)"
    >
      <span [ngClass]="labelClasses">
        {{ label }}
      </span>
      <span class="mt-0.5 text-[13px] font-bold leading-none tracking-tight">
        {{ odd }}
      </span>
      @if (subLabel) {
        <span [ngClass]="subLabelClasses">
          {{ subLabel }}
        </span>
      }
    </button>
  `,
})
export class OddsChipComponent {
  @Input({ required: true }) data!: OddsChipData;
  @Input() compact = false;

  @Output() clicked = new EventEmitter<OddsChipData>();

  get label(): string {
    return this.data?.label ?? '';
  }

  get odd(): string {
    return String(this.data?.odd ?? '');
  }

  get selected(): boolean {
    return !!this.data?.selected;
  }

  get disabled(): boolean {
    return !!this.data?.disabled;
  }

  get subLabel(): string | undefined {
    return this.data?.subLabel;
  }

  get chipClasses(): string {
    const base = 'flex flex-col items-center justify-center rounded-[var(--radius-sm)] border text-center transition-colors';
    const size = this.compact ? 'min-h-11 min-w-11 px-2 py-2' : 'min-h-14 min-w-16 px-3 py-3';
    const state = this.selected
      ? 'border-[var(--brand-green)] bg-[var(--brand-green)] text-[#06140d]'
      : 'border-[var(--brand-border)] bg-[var(--surface-1)] text-white hover:border-[var(--brand-blue)]';
    const disabled = this.disabled ? 'cursor-not-allowed opacity-50' : '';

    return [base, size, state, disabled].filter(Boolean).join(' ');
  }

  get labelClasses(): string {
    return [
      'text-[10px] font-semibold leading-none uppercase tracking-[0.06em]',
      this.selected ? 'text-[#06140d]/70' : 'text-[var(--brand-muted)]',
    ].join(' ');
  }

  get subLabelClasses(): string {
    return [
      'mt-0.5 text-[9px] font-semibold uppercase tracking-[0.08em]',
      this.selected ? 'text-[#06140d]/65' : 'text-[var(--brand-muted)]',
    ].join(' ');
  }
}
