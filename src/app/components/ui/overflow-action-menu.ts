import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, Input, Output, inject } from '@angular/core';

export interface OverflowActionMenuItem {
  id: string;
  label: string;
  icon?: string;
  tone?: 'default' | 'danger';
  disabled?: boolean;
}

@Component({
  selector: 'app-overflow-action-menu',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative inline-flex">
      <button
        type="button"
        class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-[var(--brand-border)] bg-[var(--surface-1)] text-[var(--brand-muted)] transition hover:bg-[var(--surface-2)] hover:text-white"
        (click)="toggle($event)"
      >
        <span class="material-icons text-xl">more_vert</span>
      </button>

      @if (open) {
        <div class="absolute right-0 top-[calc(100%+8px)] z-50 min-w-52 overflow-hidden rounded-[var(--radius-md)] border border-[var(--brand-border)] bg-[var(--surface-2)] shadow-brand-card">
          @for (item of items; track item.id) {
            <button
              type="button"
              class="flex w-full items-center gap-3 px-4 py-3 text-left text-sm transition hover:bg-[var(--surface-1)] disabled:cursor-not-allowed disabled:opacity-50"
              [class.text-red-200]="item.tone === 'danger'"
              [class.text-white]="item.tone !== 'danger'"
              [disabled]="item.disabled"
              (click)="choose(item, $event)"
            >
              @if (item.icon) {
                <span class="material-icons text-[18px]">{{ item.icon }}</span>
              }
              <span class="font-semibold">{{ item.label }}</span>
            </button>
          }
        </div>
      }
    </div>
  `,
})
export class OverflowActionMenuComponent {
  private readonly host = inject(ElementRef<HTMLElement>);

  @Input() items: OverflowActionMenuItem[] = [];
  @Output() action = new EventEmitter<OverflowActionMenuItem>();

  protected open = false;

  @HostListener('document:click', ['$event'])
  closeOnOutsideClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open = false;
    }
  }

  protected toggle(event: MouseEvent): void {
    event.stopPropagation();
    this.open = !this.open;
  }

  protected choose(item: OverflowActionMenuItem, event: MouseEvent): void {
    event.stopPropagation();
    if (item.disabled) {
      return;
    }

    this.action.emit(item);
    this.open = false;
  }
}
