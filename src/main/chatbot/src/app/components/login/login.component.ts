import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <i class="fas fa-comments login-icon"></i>
          <h1>C8 Banking chat</h1>
          <p class="subtitle">Welcome back</p>
        </div>
        <form (ngSubmit)="onSubmit()" class="login-form">
          <div class="form-group">
            <input 
              type="text" 
              [(ngModel)]="username" 
              name="username"
              placeholder="Enter your username"
              class="form-control"
              required
              autofocus
            >
          </div>
          <button type="submit" class="btn-login" [disabled]="!username">
            Continue
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      padding: 20px;
    }

    .login-card {
      background: white;
      border-radius: 20px;
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
      padding: 40px;
      width: 100%;
      max-width: 400px;
      text-align: center;
    }

    .login-header {
      margin-bottom: 30px;
    }

    .login-icon {
      font-size: 3rem;
      color: #333;
      margin-bottom: 15px;
    }

    h1 {
      font-size: 2rem;
      font-weight: 600;
      color: #333;
      margin: 0 0 8px 0;
      letter-spacing: -0.5px;
    }

    .subtitle {
      color: #666;
      font-size: 1rem;
      margin: 0;
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .form-group {
      text-align: left;
    }

    .form-control {
      width: 100%;
      padding: 16px 20px;
      border: 2px solid #e1e5e9;
      border-radius: 12px;
      font-size: 16px;
      transition: border-color 0.2s ease;
      background: #fafbfc;
    }

    .form-control:focus {
      outline: none;
      border-color: #333;
      background: white;
    }

    .btn-login {
      padding: 16px 20px;
      background: #333;
      color: white;
      border: none;
      border-radius: 12px;
      font-size: 16px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-login:hover:not(:disabled) {
      background: #555;
      transform: translateY(-1px);
    }

    .btn-login:disabled {
      background: #ccc;
      cursor: not-allowed;
      transform: none;
    }
  `]
})
export class LoginComponent {
  username = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    // Redirect if already logged in
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/chat']);
    }
  }

  onSubmit(): void {
    if (this.username.trim()) {
      this.authService.login(this.username.trim());
      this.router.navigate(['/chat']);
    }
  }
}