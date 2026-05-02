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
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private http = inject(HttpClient);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  loading = signal(false);
  errorLogin = signal<string | null>(null);
  errorSignup = signal<string | null>(null);
  showInscriptionSuccess = signal(false);
  hideSignupForm = signal(false);

  /** Contrôle le panel glissant : true = signup visible */
  isSignup = signal(false);

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required, Validators.minLength(6)]],
    remember: [false],
  });

  signupForm = this.fb.group({
    prenom: ['', [Validators.required, Validators.minLength(2)]],
    nom: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required, Validators.minLength(6)]],
    remember: [false],
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

  // ── Panel navigation ──────────────────────────────────────────────────────
  goToSignup(): void {
    this.isSignup.set(true);
    this.errorLogin.set(null);
  }

  goToLogin(): void {
    this.isSignup.set(false);
    this.errorSignup.set(null);
  }

  showSignupAgain(): void {
    this.hideSignupForm.set(false);
    this.showInscriptionSuccess.set(false);
    this.goToSignup();
  }

  // ── Login ─────────────────────────────────────────────────────────────────
  submitLogin(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
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
      next: ({ url }) => this.router.navigateByUrl(url),
      error: (err) => this.errorLogin.set(this.msg(err))
    });
  }

  // ── Signup (WORKFLOW COMPLET : compte + épargne + carte) ──────────────────
  submitSignup(): void {
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorSignup.set(null);

    const { prenom, nom, email, motDePasse } = this.signupForm.value;
    const payload = { prenom: prenom!, nom: nom!, email: email!, motDePasse: motDePasse! };

    console.log('═══════════════════════════════════════════════════════════');
    console.log('🚀 INSCRIPTION CLIENTE - Workflow complet');
    console.log('═══════════════════════════════════════════════════════════');
    console.log('Cliente:', prenom, nom, '|', email);

    let clientId: number;
    let userToken: string;
    let adminToken: string;
    let numCompte: string;

    const apiBase = 'http://localhost:8084/api';
    const userHeaders = () => new HttpHeaders({
      'Authorization': `Bearer ${userToken}`,
      'Content-Type': 'application/json'
    });
    const adminHeaders = () => new HttpHeaders({
      'Authorization': `Bearer ${adminToken}`,
      'Content-Type': 'application/json'
    });

    // 1) Inscription
    this.auth.register(payload)
      .pipe(
        // 2) Login cliente → on récupère userToken
        switchMap((regResponse: any) => {
          clientId = regResponse.data?.id || 0;
          console.log('✅ 1. Cliente créée (id=' + clientId + ')');
          return this.auth.login({ email: email!, motDePasse: motDePasse! });
        }),

        // 3) Login admin → on récupère adminToken
        // ⚠️ ATTENTION : ce login écrase le token cliente dans localStorage
        switchMap((loginResponse: any) => {
          userToken = loginResponse.data?.token || '';
          console.log('✅ 2. Login cliente OK (token récupéré)');
          return this.auth.login({ email: 'admin@bank.local', motDePasse: 'Admin#2025!' });
        }),

        // 4) Créer compte bancaire (cliente)
        switchMap((adminLogin: any) => {
          adminToken = adminLogin.data?.token || '';
          console.log('✅ 3. Token admin récupéré');
          return this.http.post<any>(
            `${apiBase}/comptes`,
            { intitule: 'Compte principal', devise: 'EUR' },
            { headers: userHeaders() }
          );
        }),

        // 5) Activer le compte (admin)
        switchMap((accountResponse: any) => {
          numCompte = accountResponse.numCompte;
          console.log('✅ 4. Compte créé:', numCompte);
          return this.http.post(
            `${apiBase}/comptes/${numCompte}/activer`,
            {},
            { headers: adminHeaders() }
          );
        }),

        // 6) Dépôt initial 200€ (cliente)
        switchMap(() => {
          console.log('✅ 5. Compte activé');
          return this.http.post(
            `${apiBase}/operations/deposit`,
            { numCompte, montant: 200, description: 'DEPOT_INIT' },
            { headers: userHeaders() }
          );
        }),

        // 7) Convertir en compte épargne (admin, taux 1.25%)
        switchMap(() => {
          console.log('✅ 6. Dépôt 200€ effectué');
          return this.http.post(
            `${apiBase}/savings/${numCompte}/convertir`,
            { tauxInteret: 0.0125 },
            { headers: adminHeaders() }
          );
        }),

        // 8) Émettre la carte bancaire (admin)
        switchMap((savingsResp: any) => {
          const numEp = savingsResp?.numCompteEpargne ?? '?';
          console.log('✅ 7. Compte épargne créé (num=' + numEp + ')');
          return this.http.post(
            `${apiBase}/cartes/${numCompte}/issue`,
            {},
            { headers: adminHeaders() }
          );
        }),

        // 9) Débloquer la carte (admin)
        switchMap(() => {
          console.log('✅ 8. Carte émise');
          return this.http.post(
            `${apiBase}/cartes/${numCompte}/debloquer`,
            {},
            { headers: adminHeaders() }
          );
        }),

        finalize(() => {
          this.loading.set(false);
          // ⚠️ IMPORTANT : on nettoie le localStorage dans tous les cas
          // (même en cas d'erreur, pour pas laisser le token admin traîner)
          this.auth.logout();
        })
      )
      .subscribe({
        next: () => {
          console.log('✅ 9. Carte débloquée et active');
          console.log('═══════════════════════════════════════════════════════════');
          console.log('🎉 ONBOARDING TERMINÉ AVEC SUCCÈS');
          console.log('   → Compte courant ✓');
          console.log('   → Compte épargne ✓');
          console.log('   → Carte bancaire active ✓');
          console.log('═══════════════════════════════════════════════════════════');

          // Message de succès
          this.hideSignupForm.set(true);
          this.showInscriptionSuccess.set(true);

          // Pré-remplir l'email dans le formulaire de login
          this.loginForm.patchValue({ email: email! });

          // Basculer vers le formulaire de login
          this.goToLogin();

          // Masquer le message après 8 secondes
          setTimeout(() => this.showInscriptionSuccess.set(false), 8000);
        },
        error: (err) => {
          console.log('═══════════════════════════════════════════════════════════');
          console.error('❌ ERREUR LORS DE L\'ONBOARDING');
          console.log('═══════════════════════════════════════════════════════════');
          console.error('Status:', err.status);
          console.error('URL:', err.url);
          console.error('Message:', err.message);
          console.error('Détails:', err.error);

          this.errorSignup.set(this.msg(err));
        }
      });
  }

  private msg(err: any): string {
    if (err.status === 400) {
      const msg = err?.error?.message || err?.error?.error;
      if (msg?.toLowerCase().includes('email')) {
        return 'Cet email est déjà utilisé';
      }
      return msg || 'Données invalides. Vérifiez vos informations.';
    }
    if (err.status === 0) {
      return 'Serveur inaccessible. Vérifiez que le backend est lancé sur http://localhost:8084';
    }
    return err?.error?.message ?? err?.message ?? 'Erreur réseau';
  }
}