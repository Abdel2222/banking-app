import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { finalize, of, switchMap, map, catchError } from 'rxjs';

@Component({
  selector: 'app-auth-balloons',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './auth-balloons.component.html',
  styleUrls: ['./auth-balloons.component.scss']
})
export class AuthBalloonsComponent implements OnInit {
  private fb     = inject(FormBuilder);
  private auth   = inject(AuthService);
  private http   = inject(HttpClient);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  loading                = signal(false);
  errorLogin             = signal<string | null>(null);
  errorSignup            = signal<string | null>(null);
  showInscriptionSuccess = signal(false);
  hideSignupForm         = signal(false);
  isSignup               = signal(false);

  // ✅ RGPD — affichage politique
  showRgpd = signal(false);

  loginForm = this.fb.group({
    email:      ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required, Validators.minLength(8)]],
    remember:   [false],
  });

  signupForm = this.fb.group({
    prenom:     ['', [Validators.required, Validators.minLength(2)]],
    nom:        ['', [Validators.required, Validators.minLength(2)]],
    email:      ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required, Validators.minLength(6)]],
    // ✅ RGPD — consentement obligatoire
    rgpd:       [false, [Validators.requiredTrue]],
  });

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['inscrit'] === 'true') {
        this.showInscriptionSuccess.set(true);
        this.hideSignupForm.set(true);
        setTimeout(() => this.showInscriptionSuccess.set(false), 8000);
      }
    });
  }

  goToSignup(): void { this.isSignup.set(true);  this.errorLogin.set(null); }
  goToLogin():  void { this.isSignup.set(false); this.errorSignup.set(null); }

  showSignupAgain(): void {
    this.hideSignupForm.set(false);
    this.showInscriptionSuccess.set(false);
    this.goToSignup();
  }

  // ✅ RGPD
  openRgpd(event: Event): void {
    event.preventDefault();
    this.showRgpd.set(true);
  }

  closeRgpd(): void {
    this.showRgpd.set(false);
  }

  acceptRgpd(): void {
    this.signupForm.patchValue({ rgpd: true });
    this.showRgpd.set(false);
  }

  submitLogin(): void {
    if (this.loginForm.invalid) { this.loginForm.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorLogin.set(null);

    const emailTyped = String(this.loginForm.value.email || '').trim().toLowerCase();

    this.auth.login(this.loginForm.value as any).pipe(
      switchMap(() => {
        if (emailTyped === 'admin@bank.local') return of({ url: '/admin' });
        if (this.auth.isAdmin()) return of({ url: '/admin' });
        return this.auth.me().pipe(
          map((me: any) => {
            const d = me?.data ?? me ?? {};
            const roles = [
              d.role,
              ...(Array.isArray(d.roles) ? d.roles : []),
              ...(Array.isArray(d.authorities) ? d.authorities.map((a: any) => a?.authority ?? a) : []),
            ].filter(Boolean).map((r: string) => r.toUpperCase());
            return { url: roles.some(r => r.includes('ADMIN')) ? '/admin' : '/dashboard' };
          }),
          catchError(() => of({ url: '/dashboard' }))
        );
      }),
      finalize(() => this.loading.set(false))
    ).subscribe({
      next:  ({ url }) => this.router.navigateByUrl(url),
      error: (err)     => this.errorLogin.set(this.msg(err))
    });
  }

  submitSignup(): void {
    if (this.signupForm.invalid) { this.signupForm.markAllAsTouched(); return; }

    // ✅ RGPD — vérification consentement
    if (!this.signupForm.value.rgpd) {
      this.errorSignup.set('Vous devez accepter la politique de confidentialité pour continuer.');
      return;
    }

    this.loading.set(true);
    this.errorSignup.set(null);

    const { prenom, nom, email, motDePasse } = this.signupForm.value;
    const payload = { prenom: prenom!, nom: nom!, email: email!, motDePasse: motDePasse! };

    let clientId: number;
    let userToken: string;
    let numCompte: string;

    const apiBase = 'http://localhost:8084/api';
    const userHeaders = () => new HttpHeaders({
      Authorization: `Bearer ${userToken}`,
      'Content-Type': 'application/json'
    });

    this.auth.register(payload).pipe(

      switchMap((regResponse: any) => {
        clientId = regResponse.data?.id || 0;
        return this.auth.login({ email: email!, motDePasse: motDePasse! });
      }),

      switchMap((loginResponse: any) => {
        userToken = loginResponse.data?.token || '';
        return this.http.post<any>(
          `${apiBase}/comptes`,
          { typeCompte: 'COURANT', intitule: 'Compte principal', devise: 'EUR' },
          { headers: userHeaders() }
        );
      }),

      switchMap((accountResponse: any) => {
        numCompte = accountResponse.numCompte || accountResponse.data?.numCompte;
        return this.http.post(
          `${apiBase}/operations/deposit`,
          { numCompte, montant: 200, description: 'DEPOT_INIT' },
          { headers: userHeaders() }
        ).pipe(
          catchError(() => of(null))
        );
      }),

      finalize(() => {
        this.loading.set(false);
        this.auth.logout();
      })

    ).subscribe({
      next: () => {
        this.hideSignupForm.set(true);
        this.showInscriptionSuccess.set(true);
        this.loginForm.patchValue({ email: email! });
        this.goToLogin();
        setTimeout(() => this.showInscriptionSuccess.set(false), 8000);
      },
      error: (err) => {
        this.errorSignup.set(this.msg(err));
      }
    });
  }

  private msg(err: any): string {
    if (err.status === 400) {
      const msg = err?.error?.message || err?.error?.error;
      if (msg?.toLowerCase().includes('email')) return 'Cet email est déjà utilisé';
      return msg || 'Données invalides. Vérifiez vos informations.';
    }
    if (err.status === 0) return 'Serveur inaccessible. Vérifiez que le backend est lancé sur http://localhost:8084';
    return err?.error?.message ?? err?.message ?? 'Erreur réseau';
  }
}