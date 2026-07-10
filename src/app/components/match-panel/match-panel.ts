import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { Bet, BetTarget, Match } from '../../models/resenhabet.models';

@Component({
  selector: 'app-match-panel',
  imports: [CommonModule],
  templateUrl: './match-panel.html',
  styleUrl: './match-panel.css',
})
export class MatchPanel {
  readonly match = input.required<Match | null>();
  readonly betAmount = input.required<number>();
  readonly myCurrentBet = input.required<Bet | undefined>();

  readonly betAmountChange = output<number>();
  readonly maxBet = output<void>();
  readonly placeBet = output<BetTarget>();

  protected setBetAmount(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.betAmountChange.emit(Number.isFinite(value) ? value : 0);
  }
}
