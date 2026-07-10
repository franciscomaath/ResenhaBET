import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { Bet, BetTarget, Match, ScorePayload } from '../../models/resenhabet.models';

@Component({
  selector: 'app-admin-panel',
  imports: [CommonModule],
  templateUrl: './admin-panel.html',
})
export class AdminPanel {
  readonly match = input.required<Match | null>();
  readonly currentBets = input.required<Bet[]>();

  readonly createDemoMatch = output<boolean>();
  readonly updateScore = output<ScorePayload>();
  readonly toggleBetting = output<boolean>();
  readonly resolveMatch = output<BetTarget>();
}
