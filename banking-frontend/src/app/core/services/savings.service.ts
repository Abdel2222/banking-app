import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CompteEpargne {
  id: number;
  numCompte: string;
  premierMontant: number;
  solde: number;
  balance?: number;
  statut: string;
  devise: string;
  clientId: number;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class SavingsService {
  private http = inject(HttpClient);
  private readonly API_BASE = 'http://localhost:8084/api/savings';

  /** Créer un compte épargne depuis un compte courant existant */
  convertir(numCompte: string, premierMontant: number): Observable<CompteEpargne> {
    return this.http.post<CompteEpargne>(
      `${this.API_BASE}/${numCompte}/convertir`,
      { premierMontant }
    );
  }

  /** Alimenter le compte épargne */
  alimenter(numCompte: string, montant: number): Observable<CompteEpargne> {
    return this.http.post<CompteEpargne>(
      `${this.API_BASE}/${numCompte}/alimenter`,
      { montant }
    );
  }

  /** Retirer du compte épargne */
  retirer(numCompte: string, montant: number): Observable<CompteEpargne> {
    return this.http.post<CompteEpargne>(
      `${this.API_BASE}/${numCompte}/retirer`,
      { montant }
    );
  }

  /** Détails d'un compte épargne */
  getDetails(numCompte: string): Observable<CompteEpargne> {
    return this.http.get<CompteEpargne>(`${this.API_BASE}/${numCompte}`);
  }
}
