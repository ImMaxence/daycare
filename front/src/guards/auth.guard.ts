import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../service/auth-state.service';

export const authGuard: CanActivateFn = () => {
    const authStateService = inject(AuthStateService);
    const router = inject(Router);

    if (authStateService.isAuthenticated()) {
        return true;
    }

    return router.parseUrl('/login');
};
