import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { map, catchError, tap, switchMap } from 'rxjs/operators';

export interface Client {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  dateInscription?: string;
}

export interface Compte {
  id: number;
  numCompte: string;
  balance: number;
  status: string;
  intitule: string;
  devise: string;
  clientName?: string;
  hasCard?: boolean;
  savingsAccount?: boolean;
}

export interface VirementRequest {
  numCompteSource: string;
  numCompteDestinataire: string;
  montant: number;
  communication?: string;
  description?: string;
}

export interface VirementResponse {
  success: boolean;
  message: string;
  data?: any;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class VirementService {
  private apiUrl = '/api';

  constructor(private http: HttpClient) {}

  getAllClients(): Observable<{ success: boolean; data: Client[]; message: string }> {
    const token = localStorage.getItem('auth_token');
    console.log('📡 [SERVICE] getAllClients - Token:', token ? 'PRÉSENT' : 'ABSENT');

    return this.http.get<{ success: boolean; data: Client[]; message: string }>(`${this.apiUrl}/clients`, {
      headers: { 'Authorization': `Bearer ${token}` }
    }).pipe(
      tap(response => {
        console.log('✅ [SERVICE] Clients récupérés:', response.data?.length || 0);
      }),
      catchError(error => {
        console.error('❌ [SERVICE] Erreur getAllClients:', error);
        throw error;
      })
    );
  }

  getComptesClient(clientId: number): Observable<Compte[]> {
    const token = localStorage.getItem('auth_token');
    console.log('📡 [SERVICE] getComptesClient pour client ID:', clientId);
    console.log('🔑 [SERVICE] Token:', token?.substring(0, 20) + '...');

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    });

    return this.http.get<Compte | Compte[]>(`${this.apiUrl}/comptes/client/${clientId}`, { headers })
      .pipe(
        map((response: Compte | Compte[]) => {
          const comptes = Array.isArray(response) ? response : [response];
          console.log('✅ [SERVICE] Comptes reçus pour client', clientId, ':', comptes.length);
          console.log('📋 [SERVICE] Détails:', comptes);
          return comptes;
        }),
        catchError((error: HttpErrorResponse) => {
          console.error('❌ [SERVICE] Erreur getComptesClient:', {
            clientId: clientId,
            status: error.status,
            statusText: error.statusText,
            message: error.message,
            url: error.url,
            error: error.error
          });
          return throwError(() => error);
        })
      );
  }

  // Récupérer le compte épargne
  getSavingsAccount(numCompteBancaire: string): Observable<Compte | null> {
    const token = localStorage.getItem('auth_token');
    console.log('📡 [SERVICE] getSavingsAccount pour compte:', numCompteBancaire);

    if (!token) {
      return of(null);
    }

    return this.http.get<any>(`${this.apiUrl}/savings/${numCompteBancaire}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    }).pipe(
      map(response => {
        console.log('📥 [SERVICE] Réponse API savings:', response);

        // Gérer différents formats de réponse
        const numEpargne = response.numCompteEpargne ||
                          response.num_compte_epargne ||
                          response.numeroCompteEpargne;

        const soldeEpargne = response.soldeEpargne ||
                            response.solde_epargne ||
                            response.balance ||
                            0;

        if (numEpargne) {
          console.log('✅ [SERVICE] Compte épargne trouvé:', numEpargne);
          return {
            id: 0,
            numCompte: numEpargne,
            balance: soldeEpargne,
            status: 'ACTIVE',
            intitule: 'Compte Épargne',
            devise: 'EUR',
            savingsAccount: true
          } as Compte;
        }

        console.log('⚠️ [SERVICE] Pas de numéro épargne dans la réponse');
        return null;
      }),
      catchError(error => {
        if (error.status === 404) {
          console.log('ℹ️ [SERVICE] Pas de compte épargne (404 - normal si pas converti)');
        } else {
          console.log('⚠️ [SERVICE] Erreur récupération épargne:', error.status, error.message);
        }
        return of(null);
      })
    );
  }

  // Récupérer les comptes de l'utilisateur (incluant épargne)
  getCurrentUserComptes(): Observable<Compte[]> {
    const token = localStorage.getItem('auth_token');
    console.log('📡 [SERVICE] getCurrentUserComptes - Token:', token ? 'PRÉSENT' : 'ABSENT');

    if (!token) {
      console.error('❌ [SERVICE] Pas de token d\'authentification');
      return of([]);
    }

    return this.http.get<Compte | Compte[]>(`${this.apiUrl}/comptes`, {
      headers: { 'Authorization': `Bearer ${token}` }
    }).pipe(
      switchMap(response => {
        console.log('✅ [SERVICE] Réponse /api/comptes:', response);
        const comptesBancaires = this.normalizeComptesResponse(response);

        // Si on a des comptes bancaires, vérifier s'il y a un compte épargne
        if (comptesBancaires.length > 0) {
          const numCompte = comptesBancaires[0].numCompte;
          console.log('🔍 [SERVICE] Recherche compte épargne pour:', numCompte);

          return this.getSavingsAccount(numCompte).pipe(
            map(compteEpargne => {
              if (compteEpargne) {
                console.log('✅ [SERVICE] Compte épargne ajouté automatiquement');
                console.log('📋 [SERVICE] Comptes totaux:', comptesBancaires.length + 1);
                return [...comptesBancaires, compteEpargne];
              }
              console.log('ℹ️ [SERVICE] Aucun compte épargne trouvé');
              return comptesBancaires;
            })
          );
        }

        console.log('⚠️ [SERVICE] Aucun compte bancaire trouvé');
        return of(comptesBancaires);
      }),
      catchError(error => {
        console.error('❌ [SERVICE] Erreur /api/comptes:', error);
        return of([]);
      })
    );
  }

  private normalizeComptesResponse(response: any): Compte[] {
    let comptes: any[] = [];

    if (response && response.numCompte) {
      comptes = [response];
    } else if (Array.isArray(response)) {
      comptes = response;
    } else if (response?.data) {
      comptes = Array.isArray(response.data) ? response.data : [response.data];
    } else if (response?.comptes) {
      comptes = Array.isArray(response.comptes) ? response.comptes : [response.comptes];
    }

    const normalized = comptes.map((c: any) => ({
      id: c.id || 0,
      numCompte: c.numCompte || c.numero || c.numeroCompte || '',
      balance: c.balance || c.solde || 0,
      status: c.status || c.etat || 'ACTIVATED',
      devise: c.devise || 'EUR',
      clientName: c.clientName,
      intitule: c.intitule || 'Compte Courant',
      hasCard: c.hasCard || false,
      savingsAccount: c.savingsAccount || false
    }));

    console.log('📋 [SERVICE] Comptes normalisés:', normalized);
    return normalized;
  }

  effectuerVirement(virement: VirementRequest): Observable<VirementResponse> {
    const token = localStorage.getItem('auth_token');
    console.log('💸 [SERVICE] effectuerVirement');

    if (!token) {
      console.error('❌ [SERVICE] Non authentifié');
      return throwError(() => new Error('Non authentifié'));
    }

    const virementData = {
      numCompteSource: virement.numCompteSource,
      numCompteDestinataire: virement.numCompteDestinataire,
      montant: virement.montant,
      communication: virement.communication || "Virement"
    };

    console.log('📤 [SERVICE] Données virement:', virementData);

    return this.http.post<any>(`${this.apiUrl}/operations/transfer`, virementData, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }).pipe(
      map(response => {
        console.log('✅ [SERVICE] Virement réussi:', response);
        return {
          success: true,
          message: `Virement de ${response.montant}€ effectué avec succès`,
          data: response,
          timestamp: new Date().toISOString()
        };
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('❌ [SERVICE] Erreur virement:', error);
        let errorMessage = 'Erreur lors du virement';

        if (error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.status === 403) {
          errorMessage = 'Solde insuffisant';
        } else if (error.status === 404) {
          errorMessage = 'Compte introuvable';
        } else if (error.status === 401) {
          errorMessage = 'Session expirée';
        } else if (error.status === 0) {
          errorMessage = 'Impossible de contacter le serveur';
        }

        return throwError(() => new Error(errorMessage));
      })
    );
  }

  validerMontant(montant: number): { isValid: boolean; message: string } {
    if (!montant || montant <= 0) {
      return { isValid: false, message: 'Le montant doit être supérieur à 0' };
    }
    if (montant > 10000) {
      return { isValid: false, message: 'Maximum 10 000€' };
    }
    return { isValid: true, message: 'Valide' };
  }

  isCurrentUser(clientId: number): boolean {
    return this.getCurrentUserId() === clientId;
  }

  private getCurrentUserId(): number {
    const token = localStorage.getItem('auth_token');
    if (!token) return -1;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const userId = payload.userId || payload.id || payload.sub || payload.clientId;
      return userId ? Number(userId) : -1;
    } catch {
      return -1;
    }
  }

  formatCurrency(amount: number, currency: string = 'EUR'): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: currency
    }).format(amount);
  }
}
