import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const groupAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.canManageGroup()) {
    return true;
  }

  return inject(Router).parseUrl('/');
};
