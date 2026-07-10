import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { Router } from '@angular/router';

import { OddsChipComponent, OddsChipData } from './odds-chip';

@Component({
  selector: 'app-match-row',
  standalone: true,
  imports: [CommonModule, OddsChipComponent],
  templateUrl: './match-row.html',
})
export class MatchRowComponent {
  private readonly router = inject(Router);

  @Input({ required: true }) eventId!: number;
  @Input({ required: true }) timeLabel!: string;
  @Input() statusLabel = '';
  @Input() live = false;
  @Input() compact = false;
  @Input() homeLabel = '';
  @Input() awayLabel = '';
  @Input() homeScore: number | null = null;
  @Input() awayScore: number | null = null;
  @Input() isBye = false;
  @Input() showScore = true;
  @Input() homeMuted = false;
  @Input() awayMuted = true;
  @Input() selectedOutcomeId: number | null = null;
  @Input() outcomes: OddsChipData[] = [];
  @Input() disabled = false;
  @Input() route: string | any[] | null = null;

  @Output() openMatch = new EventEmitter<number>();
  @Output() selectOutcome = new EventEmitter<{ eventId: number; outcome: OddsChipData }>();

  protected navigate(): void {
    this.openMatch.emit(this.eventId);

    if (!this.route) {
      return;
    }

    if (typeof this.route === 'string') {
      void this.router.navigateByUrl(this.route);
      return;
    }

    void this.router.navigate(this.route);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.navigate();
    }
  }

  protected chooseOutcome(outcome: OddsChipData): void {
    if (outcome.disabled || this.disabled) {
      return;
    }

    this.selectOutcome.emit({ eventId: this.eventId, outcome });
  }

  protected trackOutcome(_: number, outcome: OddsChipData): number {
    return outcome.id;
  }

  protected outcomeClass(outcome: OddsChipData): boolean {
    return this.selectedOutcomeId === outcome.id || !!outcome.selected;
  }

  protected chipData(outcome: OddsChipData): OddsChipData {
    return {
      ...outcome,
      selected: this.outcomeClass(outcome),
      disabled: outcome.disabled || this.disabled,
      compact: true,
    };
  }

  protected labelClasses(isMuted: boolean): string {
    return [
      'block truncate text-[14px] font-semibold',
      isMuted ? 'text-[var(--brand-muted)]' : 'text-white',
    ].join(' ');
  }
}
