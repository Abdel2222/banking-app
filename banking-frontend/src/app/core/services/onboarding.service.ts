import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, switchMap, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class OnboardingService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8084';

  completeOnboarding(userData: {
    prenom: string;
    nom: string;
    email: string;
    motDePasse: string;
  }): Observable<any> {

    console.log('🚀 DÉBUT WORKFLOW ONBOARDING COMPLET');

    let clientId: number;
    let userToken: string;
    let numCompte: string;

    return this.register(userData).pipe(
      tap(response => {
        clientId = response.data.id;
        console.log('✅ 1. Cliente créée (id=' + clientId + ')');
      }),

      switchMap(() => this.login(userData.email, userData.motDePasse)),
      tap(response => {
        userToken = response.data.token;
        localStorage.setItem('token', userToken);
        console.log('✅ 2. Login automatique réussi');
      }),

      switchMap(() => this.createAccount(userToken, 'Compte principal', 'EUR')),
      tap(response => {
        numCompte = response.numCompte;
        console.log('✅ 3. Compte créé:', numCompte);
      }),

      switchMap(() => this.activateAccountAsAdmin(numCompte)),
      tap(() => {
        console.log('✅ 4. Compte activé');
      }),

      switchMap(() => this.deposit(userToken, numCompte, 200, 'DEPOT_INIT')),
      tap(response => {
        console.log('✅ 5. Dépôt 200€ effectué');
        console.log('🎉 WORKFLOW ONBOARDING TERMINÉ');
      })
    );
  }

  private register(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/auth/register`, payload);
  }

  private login(email: string, motDePasse: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/auth/login`, { email, motDePasse });
  }

  private createAccount(token: string, intitule: string, devise: string): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
    return this.http.post(
      `${this.baseUrl}/api/comptes`,
      { intitule, devise },
      { headers }
    );
  }

  private activateAccountAsAdmin(numCompte: string): Observable<any> {
    return this.login('admin@bank.local', 'Admin#2025!').pipe(
      switchMap(adminLogin => {
        const headers = new HttpHeaders({
          'Authorization': `Bearer ${adminLogin.data.token}`,
          'Content-Type': 'application/json'
        });
        return this.http.post(
          `${this.baseUrl}/api/comptes/${numCompte}/activer`,
          {},
          { headers }
        );
      })
    );
  }

  private deposit(token: string, numCompte: string, montant: number, description: string): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
    return this.http.post(
      `${this.baseUrl}/api/operations/deposit`,
      { numCompte, montant, description },
      { headers }
    );
  }
}
