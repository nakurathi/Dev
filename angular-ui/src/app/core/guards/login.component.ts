import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule }   from '@angular/material/card';
import { MatInputModule }  from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatInputModule, MatButtonModule],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Healthcare Platform</mat-card-title>
          <mat-card-subtitle>Sign in to continue</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" placeholder="you@example.com" />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password" type="password" />
            </mat-form-field>

            @if (error) {
              <p class="error-msg">{{ error }}</p>
            }

            <button mat-raised-button color="primary" type="submit"
                    class="full-width" [disabled]="loginForm.invalid || loading">
              {{ loading ? 'Signing in…' : 'Sign In' }}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container { display:flex; justify-content:center; align-items:center; height:100vh; background:#f5f5f5; }
    .login-card      { width:380px; padding:24px; }
    .full-width      { width:100%; margin-bottom:16px; }
    .error-msg       { color:#f44336; font-size:13px; margin-bottom:12px; }
  `]
})
export class LoginComponent {
  private readonly fb     = inject(FormBuilder);
  private readonly router = inject(Router);

  loading = false;
  error   = '';

  loginForm = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  onSubmit(): void {
    if (this.loginForm.invalid) return;
    this.loading = true;
    this.error   = '';

    // TODO: Replace with real auth service call (GCP Identity Platform / OIDC)
    const { email, password } = this.loginForm.value;
    if (email === 'admin@healthcare.com' && password === 'admin') {
      // Mock JWT — replace with real token from identity provider
      localStorage.setItem('access_token', 'mock.jwt.token');
      this.router.navigate(['/dashboard']);
    } else {
      this.error   = 'Invalid credentials. Please try again.';
      this.loading = false;
    }
  }
}
