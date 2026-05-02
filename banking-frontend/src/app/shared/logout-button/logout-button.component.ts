// src/app/shared/components/logout-button.component.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-logout-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button *ngIf="logged()" (click)="logout()" class="btn-logout" aria-label="Se déconnecter">
      Se déconnecter
    </button>
  `,
  styles: [`
    :host { display: inline-block; }
    .btn-logout {
      padding:.5rem 1rem; border:0; border-radius:.5rem;
      background:#ef4444; color:#fff; cursor:pointer; font-weight:600;
    }
    .btn-logout:hover { opacity:.9; }
  `]
})
export class LogoutButtonComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  logged = signal<boolean>(false);

  ngOnInit() {
    // utilise le getter token existant
    this.logged.set(!!this.auth.token);
  }

  logout() {
    this.auth.logout();
    this.logged.set(false);
    this.router.navigateByUrl('/auth');
  }
}
