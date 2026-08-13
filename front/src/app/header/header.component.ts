import { Component, OnInit, inject } from '@angular/core';
import { AuthStateService } from '../../service/auth-state.service';

@Component({
    selector: 'app-header',
    imports: [],
    templateUrl: './header.component.html',
    styleUrl: './header.component.scss',
})
export class HeaderComponent implements OnInit {
    private readonly authStateService = inject(AuthStateService);

    readonly currentUser = this.authStateService.currentUser;

    ngOnInit(): void {
        if (!this.currentUser()) {
            this.authStateService.loadCurrentUser().subscribe();
        }
    }
}
