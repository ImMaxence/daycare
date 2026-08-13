import { Component, OnInit, computed, inject } from '@angular/core';
import { format } from 'date-fns';
import { Router } from '@angular/router';
import { AuthStateService } from '../../service/auth-state.service';

@Component({
    selector: 'app-header',
    imports: [],
    templateUrl: './header.component.html',
    styleUrl: './header.component.scss',
})
export class HeaderComponent implements OnInit {
    private readonly authStateService = inject(AuthStateService);
    private readonly router = inject(Router);

    readonly currentUser = this.authStateService.currentUser;

    readonly avatarInitial = computed(() => this.currentUser()?.username?.charAt(0).toUpperCase() ?? '');
    readonly avatarColor = computed(() => this.stringToColor(this.currentUser()?.username ?? ''));

    readonly lastConnexionLabel = computed(() => {
        const lastConnexion = this.currentUser()?.lastConnexion;
        return lastConnexion ? format(new Date(lastConnexion), 'dd/MM/yyyy à HH:mm') : 'Jamais connecté';
    });

    ngOnInit(): void {
        if (!this.currentUser()) {
            this.authStateService.loadCurrentUser().subscribe();
        }
    }

    logout(): void {
        this.authStateService.logout();
        this.router.navigateByUrl('/login');
    }

    // deterministic pastel-ish color derived from the username so it stays stable across sessions
    private stringToColor(value: string): string {
        let hash = 0;
        for (let i = 0; i < value.length; i++) {
            hash = value.charCodeAt(i) + ((hash << 5) - hash);
        }
        return `hsl(${hash % 360}, 60%, 45%)`;
    }
}
