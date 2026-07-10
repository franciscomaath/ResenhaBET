import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const claimPlayerGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return router.parseUrl('/');
  }

  const currentGroupId = auth.currentGroupId();
  const routeGroupId = Number(route.paramMap.get('groupId'));

  if (!currentGroupId || !Number.isFinite(routeGroupId) || routeGroupId !== currentGroupId) {
    return router.parseUrl('/');
  }

  if (auth.currentGroupPlayerClaimed()) {
    return router.parseUrl('/');
  }

  return true;
};
