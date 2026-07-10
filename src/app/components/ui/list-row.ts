import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-list-row',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [ngClass]="rowClasses">
      <div class="min-w-0 flex-1">
        <div class="flex items-start gap-3">
          <ng-content select="[leading]"></ng-content>

          <div class="min-w-0 flex-1">
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0 flex-1">
                <ng-content select="[title]"></ng-content>
                <ng-content select="[subtitle]"></ng-content>
              </div>
              <div class="shrink-0">
                <ng-content select="[trailing]"></ng-content>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ListRowComponent {
  @Input() interactive = false;
  @Input() selected = false;
  @Input() dense = false;

  get rowClasses(): string {
    const base = 'rounded-[var(--radius-md)] border border-[var(--brand-border)] bg-[var(--surface-2)] px-4';
    const dense = this.dense ? 'py-2.5' : 'py-3';
    const interactive = this.interactive ? 'transition-colors hover:bg-[var(--surface-1)]' : '';
    const selected = this.selected ? 'ring-1 ring-[var(--brand-blue)] bg-[rgba(61,139,253,0.08)]' : '';

    return [base, dense, interactive, selected].filter(Boolean).join(' ');
  }
}
