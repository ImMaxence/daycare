import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { LOCAL_STORAGE_REFRESH_TOKEN_KEY, LOCAL_STORAGE_TOKEN_KEY } from '../utils/constants';
import { AuthStateService } from './auth-state.service';

const withAuthHeader = (req: Parameters<HttpInterceptorFn>[0], token: string) =>
    req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    });

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authStateService = inject(AuthStateService);
    const token = localStorage.getItem(LOCAL_STORAGE_TOKEN_KEY);

    const request = token ? withAuthHeader(req, token) : req;

    return next(request).pipe(
        catchError((error) => {
            const hasRefreshToken = !!localStorage.getItem(LOCAL_STORAGE_REFRESH_TOKEN_KEY);

            if (error.status !== 401 || !hasRefreshToken) {
                return throwError(() => error);
            }

            return authStateService.refreshToken().pipe(
                switchMap((tokens) => next(withAuthHeader(req, tokens.accessToken ?? ''))),
                catchError((refreshError) => {
                    authStateService.logout();
                    return throwError(() => refreshError);
                })
            );
        })
    );
};