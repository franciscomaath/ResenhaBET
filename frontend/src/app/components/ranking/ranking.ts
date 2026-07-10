import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';

import { Bettor } from '../../models/resenhabet.models';

@Component({
  selector: 'app-ranking',
  imports: [CommonModule],
  templateUrl: './ranking.html',
})
export class Ranking {
  readonly bettors = input.required<Bettor[]>();
  readonly currentUser = input.required<string>();
}
