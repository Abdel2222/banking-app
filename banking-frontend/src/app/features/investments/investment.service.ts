import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type StatutPlacement = 'ACTIF' | 'CLOTURE' | 'ANNULE';
export type TypeFonds = 'CRYPTO' | 'SECURITE' | 'EQUILIBRE' | 'ACTIONS' | 'IMMOBILIER';

export interface Fonds {
  id: number;
  nomFonds: string;
  codeIdentification: string;
  rendement: number;
  montant: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreatePlacementRequest {
  compteBancaireId: number;
  fondsId: number;
  montant: number;
}

export interface Placement {
  id: number;
  type?: TypeFonds | string;
  codeIdentification?: string;
  nomFonds: string;
  codeFonds?: string;
  numeroCompte: string;
  clientId: number;
  clientNomComplet: string;
  montant: number;
  rendement: number;          // ⭐ renommé (avant : rendementPrevu)
  gainPrevu: number;
  valeurEstimee: number;
  datePlacement: string;
  dateCloture?: string;
  dateSortie?: string;
  fraisSortie?: number;
  statut: StatutPlacement;
}

@Injectable({ providedIn: 'root' })
export class InvestmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8084/api';

  /* ===== FONDS ===== */

  getTousLesFonds(): Observable<Fonds[]> {
    return this.http.get<Fonds[]>(`${this.apiUrl}/fonds`);
  }

  /** Alias pour compatibilité — désormais identique à getTousLesFonds() puisque tous les fonds sont actifs */
  getFondsActifs(): Observable<Fonds[]> {
    return this.http.get<Fonds[]>(`${this.apiUrl}/fonds`);
  }

  getFondsById(id: number): Observable<Fonds> {
    return this.http.get<Fonds>(`${this.apiUrl}/fonds/${id}`);
  }

  /* ===== PLACEMENTS ===== */

  createPlacement(request: CreatePlacementRequest): Observable<Placement> {
    return this.http.post<Placement>(`${this.apiUrl}/placements`, request);
  }

  getTousLesPlacements(): Observable<Placement[]> {
    return this.http.get<Placement[]>(`${this.apiUrl}/placements`);
  }

  getPlacementById(id: number): Observable<Placement> {
    return this.http.get<Placement>(`${this.apiUrl}/placements/${id}`);
  }

  getPlacementsByClient(clientId: number): Observable<Placement[]> {
    return this.http.get<Placement[]>(`${this.apiUrl}/placements/client/${clientId}`);
  }

  getPlacementsActifsByClient(clientId: number): Observable<Placement[]> {
    return this.http.get<Placement[]>(`${this.apiUrl}/placements/client/${clientId}/actifs`);
  }

  cloturerPlacement(id: number): Observable<Placement> {
    return this.http.patch<Placement>(`${this.apiUrl}/placements/${id}/cloturer`, {});
  }

  sortirAvantEcheance(id: number): Observable<Placement> {
    return this.http.post<Placement>(`${this.apiUrl}/placements/${id}/sortir`, {});
  }

  annulerPlacement(id: number): Observable<Placement> {
    return this.http.patch<Placement>(`${this.apiUrl}/placements/${id}/annuler`, {});
  }
}
