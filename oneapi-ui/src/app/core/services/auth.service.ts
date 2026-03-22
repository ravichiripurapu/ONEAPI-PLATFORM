import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { User, LoginRequest, LoginResponse } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser$: Observable<User | null>;
  public currentUserSignal = signal<User | null>(null);

  constructor(private http: HttpClient) {
    const storedUser = this.getStoredUser();
    this.currentUserSubject = new BehaviorSubject<User | null>(storedUser);
    this.currentUser$ = this.currentUserSubject.asObservable();
    this.currentUserSignal.set(storedUser);
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  login(username: string, password: string, rememberMe: boolean = false): Observable<User> {
    const loginRequest: LoginRequest = {
      username,
      password,
      rememberMe
    };

    return this.http.post<LoginResponse>(`${environment.apiUrl}/authenticate`, loginRequest)
      .pipe(
        map(response => {
          // Store JWT token
          this.setToken(response.id_token);
          // Decode token and create user object
          const user = this.decodeToken(response.id_token);
          this.setUser(user);
          return user;
        })
      );
  }

  private decodeToken(token: string): User {
    try {
      // JWT tokens have three parts separated by dots
      const parts = token.split('.');
      if (parts.length !== 3) {
        throw new Error('Invalid token format');
      }

      // Decode the payload (second part)
      const payload = JSON.parse(atob(parts[1]));

      // Extract user information from token payload
      const roles = payload.auth ? payload.auth.split(',').map((role: string) => ({
        id: 0,
        name: role
      })) : [];

      return {
        id: 0, // Token doesn't contain user ID
        login: payload.sub || '',
        email: payload.email || `${payload.sub}@localhost`,
        firstName: payload.firstName || '',
        lastName: payload.lastName || '',
        activated: true,
        roles: roles
      };
    } catch (error) {
      console.error('Error decoding token:', error);
      throw new Error('Failed to decode authentication token');
    }
  }

  logout(): void {
    localStorage.removeItem(environment.tokenKey);
    localStorage.removeItem(environment.userKey);
    this.currentUserSubject.next(null);
    this.currentUserSignal.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(environment.tokenKey);
  }

  setToken(token: string): void {
    localStorage.setItem(environment.tokenKey, token);
  }

  private getStoredUser(): User | null {
    const userJson = localStorage.getItem(environment.userKey);
    return userJson ? JSON.parse(userJson) : null;
  }

  private setUser(user: User): void {
    localStorage.setItem(environment.userKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
    this.currentUserSignal.set(user);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasRole(roleName: string): boolean {
    const user = this.currentUserValue;
    return user?.roles.some(r => r.name === roleName) || false;
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }
}
