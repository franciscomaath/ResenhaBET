import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const activeGroupGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return true;
  }

  const groupId = auth.currentGroupId();
  if (groupId && !auth.currentGroupPlayerClaimed()) {
    return router.parseUrl(`/groups/${groupId}/claim-player`);
  }

  if (groupId) {
    return true;
  }

  if (route.routeConfig?.path === 'groups') {
    return true;
  }

  return router.parseUrl('/groups');
};
