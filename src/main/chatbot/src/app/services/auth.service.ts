import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly STORAGE_KEY = 'chatbot_user';

  login(username: string): void {
    localStorage.setItem(this.STORAGE_KEY, username);
  }

  logout(): void {
    localStorage.removeItem(this.STORAGE_KEY);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem(this.STORAGE_KEY);
  }

  getCurrentUser(): string | null {
    return localStorage.getItem(this.STORAGE_KEY);
  }
}