import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-market-accordion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './market-accordion.html',
})
export class MarketAccordionComponent {
  @Input({ required: true }) title!: string;
  @Input() open = false;
  @Input() status?: string;
  @Input() compact = false;
  @Input() icon?: string;
  @Input() iconClass = 'text-[var(--text-muted)]';
  @Input() variant: 'card' | 'flat' = 'card';

  get sectionClasses(): string {
    return this.variant === 'flat'
      ? 'border-b border-[var(--border-hairline)]'
      : 'overflow-hidden rounded-[var(--radius-lg)] border border-[var(--border-hairline)] bg-[var(--surface-2)] shadow-brand-card';
  }

  get contentClasses(): string {
    return this.variant === 'flat' ? 'px-4 pb-4 pt-1' : 'bg-[var(--surface-1)] px-4 pb-4 pt-3';
  }

  @Output() openChange = new EventEmitter<boolean>();

  protected toggle(): void {
    this.openChange.emit(!this.open);
  }
}
