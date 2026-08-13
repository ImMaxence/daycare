import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { AuthStateService } from "../../service/auth-state.service";

@Component({
  selector: "app-login.component",
  imports: [FormsModule],
  templateUrl: "./login.component.html",
  styleUrl: "./login.component.scss",
})
export class LoginComponent {
  private readonly authStateService = inject(AuthStateService);
  private readonly router = inject(Router);

  username = "";
  password = "";
  readonly hasError = signal("");

  login(): void {
    this.hasError.set("");

    this.authStateService.login(this.username, this.password).subscribe({
      next: () => this.router.navigateByUrl("/map"),
      error: (e) => this.hasError.set(e.message),
    });
  }
}
