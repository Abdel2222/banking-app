import { Injectable } from '@angular/core';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { jwtDecode } from 'jwt-decode';

type Api<T> = { success: boolean; message?: string; data?: T };

export interface AuthPayload {
  email: string;
  motDePasse: string;
  nom?: string;
  prenom?: string;
}

export interface AuthResponse {
  token: string;
  user?: any;
}

interface JwtPayload {
  sub?: string; // id client réel dans ton JWT
  email?: string;
  nomComplet?: string;
  role?: string;
  roles?: string[];
  exp?: number;
  iat?: number;
}

// Clé cohérente avec token.interceptor.ts
const TOKEN_KEY = 'auth_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = 'http://localhost:8084/api/auth';

  constructor(private http: HttpClient) {
    console.log('[AUTH_SERVICE] Service initialized');
    this.checkTokenExpiration();
  }

  register(body: AuthPayload): Observable<Api<AuthResponse>>;
  register(email: string, motDePasse: string, nom?: string, prenom?: string): Observable<Api<AuthResponse>>;
  register(a: any, b?: any, c?: any, d?: any): Observable<Api<AuthResponse>> {
    const payload: AuthPayload =
      typeof a === 'string'
        ? { email: a, motDePasse: b, nom: c, prenom: d }
        : a;

    console.log('[AUTH_SERVICE] Register attempt for:', payload.email);

    return this.http.post<any>(`${this.base}/register`, payload)
      .pipe(
        tap(res => {
          const token =
            res?.data?.token ||
            res?.token ||
            res?.accessToken ||
            res?.data?.accessToken ||
            null;

          console.log('[AUTH_SERVICE] Register response:', res);
          console.log('[AUTH_SERVICE] TOKEN EXTRAIT REGISTER =', token);

          this.maybeStoreToken(token || undefined);
        }),
        catchError(error => {
          console.error('[AUTH_SERVICE] Register error:', error);
          return throwError(() => error);
        })
      );
  }

  login(body: { email: string; motDePasse: string }): Observable<Api<AuthResponse>>;
  login(email: string, motDePasse: string): Observable<Api<AuthResponse>>;
  login(a: any, b?: any): Observable<Api<AuthResponse>> {
    const payload = typeof a === 'string' ? { email: a, motDePasse: b } : a;

    console.log('[AUTH_SERVICE] Login attempt for:', payload.email);

    return this.http.post<any>(`${this.base}/login`, payload)
      .pipe(
        tap(res => {
          const token =
            res?.data?.token ||
            res?.token ||
            res?.accessToken ||
            res?.data?.accessToken ||
            null;

          console.log('[AUTH_SERVICE] FULL LOGIN RESPONSE =', res);
          console.log('[AUTH_SERVICE] TOKEN EXTRAIT =', token);

          this.maybeStoreToken(token || undefined);
        }),
        catchError(error => {
          console.error('[AUTH_SERVICE] Login error:', error);
          return throwError(() => error);
        })
      );
  }

  me(): Observable<Api<any>> {
    console.log('[AUTH_SERVICE] Getting user profile');

    return this.http.get<Api<any>>(`${this.base}/me`).pipe(
      tap(response => {
        console.log('[AUTH_SERVICE] Profile response:', response);
      }),
      catchError(error => {
        console.error('[AUTH_SERVICE] Profile error:', error);

        if (error.status === 401) {
          console.log('[AUTH_SERVICE] Profile request unauthorized, logging out');
          this.logout();
        }

        return throwError(() => error);
      })
    );
  }

  logout(): void {
    console.log('[AUTH_SERVICE] Logging out user');
    const isBrowser = typeof window !== 'undefined' && typeof localStorage !== 'undefined';
    if (isBrowser) {
      localStorage.removeItem(TOKEN_KEY);
    }
    console.log('[AUTH_SERVICE] User logged out successfully');
  }

  get token(): string | null {
    const isBrowser = typeof window !== 'undefined' && typeof localStorage !== 'undefined';
    return isBrowser ? localStorage.getItem(TOKEN_KEY) : null;
  }

  currentUser(): JwtPayload | null {
    const token = this.token;
    if (!token) {
      return null;
    }

    try {
      const decoded = jwtDecode<JwtPayload>(token);

      if (decoded.exp && decoded.exp * 1000 < Date.now()) {
        console.log('[AUTH_SERVICE] Token expired, removing');
        this.logout();
        return null;
      }

      return decoded;
    } catch (error) {
      console.error('[AUTH_SERVICE] Error decoding token:', error);
      this.logout();
      return null;
    }
  }

  get clientIdFromToken(): number | null {
    const u = this.currentUser();
    if (!u?.sub) return null;

    const n = Number(u.sub);
    return isNaN(n) ? null : n;
  }

  role(): string | null {
    const u = this.currentUser();
    if (!u) return null;
    const r = u.role ?? (Array.isArray(u.roles) ? u.roles[0] : null);
    return r ?? null;
  }

  isLoggedIn(): boolean {
    const user = this.currentUser();
    const isLoggedIn = !!user;
    console.log(`[AUTH_SERVICE] isLoggedIn check: ${isLoggedIn}`);
    return isLoggedIn;
  }

  isAdmin(): boolean {
    const r = (this.role() || '').toUpperCase();
    const isAdmin = r === 'ADMIN' || r === 'ROLE_ADMIN' || r === 'SUPER_ADMIN' || r.includes('ADMIN');
    console.log(`[AUTH_SERVICE] isAdmin check: ${isAdmin} (role: ${r})`);
    return isAdmin;
  }

  private maybeStoreToken(token?: string): void {
    if (token) {
      const isBrowser = typeof window !== 'undefined' && typeof localStorage !== 'undefined';
      if (isBrowser) {
        localStorage.setItem(TOKEN_KEY, token);
        console.log('[AUTH_SERVICE] Token stored successfully');
      }
    }
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  }

  private checkTokenExpiration(): void {
    const user = this.currentUser();
    if (!user) {
      console.log('[AUTH_SERVICE] No valid user at startup');
      return;
    }

    if (user.exp) {
      const expirationDate = new Date(user.exp * 1000);
      const now = new Date();

      console.log('[AUTH_SERVICE] Token expires at:', expirationDate);
      console.log('[AUTH_SERVICE] Current time:', now);

      if (expirationDate <= now) {
        console.log('[AUTH_SERVICE] Token expired at startup, logging out');
        this.logout();
      } else {
        const timeUntilExpiry = expirationDate.getTime() - now.getTime();
        console.log(`[AUTH_SERVICE] Token valid for ${Math.round(timeUntilExpiry / 1000 / 60)} more minutes`);
      }
    }
  }
}
