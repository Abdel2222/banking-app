import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth-smoke',
  standalone: true,
  imports: [CommonModule],
  template: `
  <div style="max-width:720px;margin:40px auto;font-family:system-ui">
    <h2>Auth Smoke Test (Front → Backend)</h2>
    <p style="color:#555">
      Ce test va <b>créer un compte</b> avec un email aléatoire, puis <b>se connecter</b> et enfin appeler <code>/api/auth/me</code>.
    </p>

    <div style="display:flex;gap:8px;flex-wrap:wrap;margin:12px 0">
      <button (click)="runFull()" [disabled]="running()">▶️ Lancer le test complet</button>
      <button (click)="registerOnly()" [disabled]="running()">🆕 Register seul</button>
      <button (click)="loginOnly()" [disabled]="running()">🔐 Login seul</button>
      <button (click)="clear()" [disabled]="running()">🧹 Clear</button>
    </div>

    <div style="margin:10px 0;padding:10px;background:#f6f6f6;border:1px solid #e5e5e5;border-radius:6px">
      <div>Dernier email généré : <b>{{ email() || '—' }}</b></div>
      <div>Token (localStorage) : <code>{{ token() ? 'présent ✅' : 'absent ❌' }}</code></div>
    </div>

    <h3>Logs</h3>
    <pre style="white-space:pre-wrap;background:#111;color:#e6e6e6;padding:12px;border-radius:6px;max-height:360px;overflow:auto">
{{ logs() }}
    </pre>
  </div>
  `,
})
export class AuthSmokeComponent {
  private auth = inject(AuthService);

  running = signal(false);
  email   = signal<string | null>(null);
  token   = signal<string | null>(localStorage.getItem('token'));
  logs    = signal<string>('');

  private log(line: string, obj?: any) {
    const t = new Date().toISOString().substring(11, 19);
    this.logs.set(this.logs() + `[${t}] ${line}\n` + (obj ? JSON.stringify(obj, null, 2) + '\n' : ''));
  }

  private randomEmail() {
    return `fronttest+${Date.now()}${Math.floor(Math.random()*1000)}@bank.local`;
  }

  async runFull() {
    this.clear();
    this.running.set(true);
    try {
      await this.registerOnly();
      await this.loginOnly();
      this.log('GET /api/auth/me ...');
      const me = await firstValueFrom(this.auth.me());
      this.log('Réponse /me ✅', me);
    } catch (e:any) {
      this.log('❌ Erreur dans le scénario', e?.error ?? e);
    } finally {
      this.running.set(false);
      this.token.set(localStorage.getItem('token'));
    }
  }

  async registerOnly() {
    if (!this.email()) this.email.set(this.randomEmail());
    const payload = {
      prenom: 'Front',
      nom: 'Smoke',
      email: this.email()!,
      motDePasse: 'Azerty#123',
    };
    this.log('POST /api/auth/register ...', payload);
    const res = await firstValueFrom(this.auth.register(payload));
    this.log('Register ✅', res);
  }

  async loginOnly() {
    if (!this.email()) {
      this.log('⚠️ Login: aucun email généré encore. Lance d’abord Register ou Run Full.');
      return;
    }
    const creds = { email: this.email()!, motDePasse: 'Azerty#123' };
    this.log('POST /api/auth/login ...', creds);
    const res = await firstValueFrom(this.auth.login(creds));
    this.log('Login ✅', res);
  }

  clear() {
    this.logs.set('');
    this.email.set(null);
    localStorage.removeItem('token');
    this.token.set(null);
  }
}
