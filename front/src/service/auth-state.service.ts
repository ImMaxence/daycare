import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthService } from './api/api/auth.service';
import { UserService } from './api/api/user.service';
import { TokenResponse } from './api/model/tokenResponse';
import { UserResponse } from './api/model/userResponse';
import { LOCAL_STORAGE_REFRESH_TOKEN_KEY, LOCAL_STORAGE_TOKEN_KEY } from '../utils/constants';

@Injectable({
    providedIn: 'root'
})
export class AuthStateService {
    private readonly authService = inject(AuthService);
    private readonly userService = inject(UserService);

    readonly currentUser = signal<UserResponse | null>(null);

    login(username: string, password: string): Observable<TokenResponse> {
        return this.authService.login({ username, password }).pipe(
            tap((tokens) => this.storeTokens(tokens))
        );
    }

    refreshToken(): Observable<TokenResponse> {
        const refreshToken = localStorage.getItem(LOCAL_STORAGE_REFRESH_TOKEN_KEY) ?? '';
        return this.authService.refresh({ refreshToken }).pipe(
            tap((tokens) => this.storeTokens(tokens))
        );
    }

    loadCurrentUser(): Observable<UserResponse> {
        return this.userService.getCurrentUser().pipe(
            tap((user) => this.currentUser.set(user))
        );
    }

    logout(): void {
        localStorage.removeItem(LOCAL_STORAGE_TOKEN_KEY);
        localStorage.removeItem(LOCAL_STORAGE_REFRESH_TOKEN_KEY);
        this.currentUser.set(null);
    }

    isAuthenticated(): boolean {
        return !!localStorage.getItem(LOCAL_STORAGE_TOKEN_KEY);
    }

    private storeTokens(tokens: TokenResponse): void {
        if (tokens.accessToken) {
            localStorage.setItem(LOCAL_STORAGE_TOKEN_KEY, tokens.accessToken);
        }
        if (tokens.refreshToken) {
            localStorage.setItem(LOCAL_STORAGE_REFRESH_TOKEN_KEY, tokens.refreshToken);
        }
    }
}
