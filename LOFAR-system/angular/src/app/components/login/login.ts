import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  username = '';
  password = '';
  loading = signal(false);
  errorMsg = signal<string | null>(null);
  showPassword = signal(false);

  private returnUrl = '/sensors';

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // If already logged in, redirect away
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/sensors']);
    }
    // Capture returnUrl from query params (set by auth guard)
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/sensors';
  }

  onSubmit(): void {
    if (!this.username.trim() || !this.password.trim()) {
      this.errorMsg.set('Please enter both username and password.');
      return;
    }

    this.loading.set(true);
    this.errorMsg.set(null);

    this.auth.login(this.username.trim(), this.password.trim()).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate([this.returnUrl]);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401 || err.status === 403) {
          this.errorMsg.set('Invalid username or password.');
        } else {
          this.errorMsg.set('Connection error. Is the backend running?');
        }
      }
    });
  }

  togglePassword(): void {
    this.showPassword.set(!this.showPassword());
  }
}