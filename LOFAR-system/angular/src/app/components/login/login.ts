import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  username = '';
  password = '';
  loading = signal(false);
  errorMsg = signal<string | null>(null);
  showPassword = signal(false);

  private returnUrl = '/home';

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/home']);
    }
  
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/home';
  }

  onSubmit(): void {

    if (!this.username.trim() || !this.password.trim()) {
      this.errorMsg.set('Please enter both username and password.');
      return;
    }

    this.loading.set(true);
    this.errorMsg.set(null);

    this.auth.login(this.username.trim(), this.password.trim()).subscribe({
      next: (user) => {
        this.loading.set(false);
        if (user.role === 'ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/home']);
        }
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