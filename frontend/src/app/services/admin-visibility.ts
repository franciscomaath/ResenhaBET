import { Injectable, inject } from '@angular/core';

import { ResenhaBetState } from './resenhabet-state';

@Injectable({ providedIn: 'root' })
export class AdminVisibility {
  private readonly state = inject(ResenhaBetState);

  readonly canManage = this.state.isAdmin;
}
