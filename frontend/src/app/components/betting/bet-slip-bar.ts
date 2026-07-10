import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-bet-slip-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bet-slip-bar.html',
})
export class BetSlipBarComponent {
  @Input() empty = true;
  @Input() expanded = false;
  @Input() hasFab = false;
  @Input() itemCount = 0;
  @Input() combinedOdd = 0;
  @Input() warnings = false;
  @Input() title = 'Carrinho de Apostas';
  @Input() summaryLabel = 'Carrinho';
  @Input() stake = 0;
  @Input() walletBalance = 0;
  @Input() potentialReturn = 0;
  @Input() submitLabel = 'Apostar';
  @Input() submitBusyLabel = 'Apostando...';
  @Input() submitting = false;
  @Input() errorMessage = '';

  @Output() toggleExpanded = new EventEmitter<void>();
  @Output() submit = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();
  @Output() maxStake = new EventEmitter<void>();
  @Output() stakeChange = new EventEmitter<number>();

  protected onStakeInput(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.stakeChange.emit(Number.isFinite(value) ? value : 0);
  }

  get collapsedClasses(): string {
    return [
      'fixed z-40 flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--brand-border)] bg-[var(--surface-2)] px-4 py-3 text-left shadow-brand-card transition hover:bg-[var(--surface-1)]',
      this.hasFab ? 'bottom-[calc(144px+var(--safe-area-bottom))]' : 'bottom-[calc(72px+var(--safe-area-bottom))]',
      'right-4',
    ].join(' ');
  }

  get expandedClasses(): string {
    return 'fixed bottom-0 left-0 right-0 z-40 flex max-h-[70vh] flex-col border-t border-[var(--brand-border)] bg-[var(--surface-2)] shadow-brand-bottom-sheet pb-safe';
  }
}
