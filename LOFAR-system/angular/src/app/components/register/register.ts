import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-register',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  username = '';
  password = '';
  confirmPassword = '';
  email = '';
  loading = signal(false);
  errorMsg = signal<string | null>(null);
  successMsg = signal<string | null>(null);

  private readonly apiBase = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient, private router: Router) {}

  onSubmit(): void {
    this.errorMsg.set(null);
    this.successMsg.set(null);

    if (!this.username.trim() || !this.password.trim() || !this.email.trim()) {
      this.errorMsg.set('Please fill in all fields.');
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMsg.set('Passwords do not match.');
      return;
    }

    if (this.password.length < 4) {
      this.errorMsg.set('Password must be at least 4 characters.');
      return;
    }

    this.loading.set(true);

    this.http.post<any>(`${this.apiBase}/register`, {
      username: this.username.trim(),
      password: this.password,
      email: this.email.trim()
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.successMsg.set(`Account created for ${res.username}! Redirecting to login…`);
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.error ?? 'Registration failed. Please try again.');
      }
    });
  }
}