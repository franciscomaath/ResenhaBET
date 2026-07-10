import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface TabBarItem {
  id: string;
  label: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-tab-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav class="hide-scrollbar flex overflow-x-auto border-b border-[var(--brand-border)]">
      @for (item of items; track item.id) {
        <button
          type="button"
          class="relative shrink-0 px-4 py-3 text-[13px] font-semibold transition-colors"
          [class.text-white]="item.id === activeId"
          [class.text-[var(--brand-muted)]]="item.id !== activeId"
          [class.cursor-not-allowed]="item.disabled"
          [disabled]="item.disabled"
          (click)="select(item.id)"
        >
          {{ item.label }}
          @if (item.id === activeId) {
            <span class="absolute inset-x-4 -bottom-px h-0.5 rounded-t bg-[var(--brand-blue)]"></span>
          }
        </button>
      }
    </nav>
  `,
})
export class TabBarComponent {
  @Input() items: TabBarItem[] = [];
  @Input() activeId = '';
  @Output() activeIdChange = new EventEmitter<string>();

  protected select(id: string): void {
    if (id !== this.activeId) {
      this.activeIdChange.emit(id);
    }
  }
}
