import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type NiveauRisque = 'FAIBLE' | 'MOYEN' | 'ELEVE';
export type StatutPlacement = 'ACTIF' | 'CLOTURE' | 'ANNULE';

export interface Fonds {
  id: number;
  nomFonds: string;
  codeFonds?: string;
  description?: string;
  rendement: number;
  niveauRisque: NiveauRisque;
  montantMinimum: number;
  disponible?: boolean;
  actif?: boolean;
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
  clientId: number;
  clientNomComplet: string;
  compteBancaireId: number;
  numeroCompte: string;
  fondsId: number;
  nomFonds: string;
  montant: number;
  rendementPrevu: number;
  gainPrevu: number;
  valeurEstimee: number;
  datePlacement: string;
  dateCloture: string | null;
  statut: StatutPlacement;
}

@Injectable({
  providedIn: 'root'
})
export class InvestmentService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8084/api';

  getTousLesFonds(): Observable<Fonds[]> {
    return this.http.get<Fonds[]>(`${this.apiUrl}/fonds`);
  }

  getFondsActifs(): Observable<Fonds[]> {
    return this.http.get<Fonds[]>(`${this.apiUrl}/fonds/actifs`);
  }

  getFondsById(id: number): Observable<Fonds> {
    return this.http.get<Fonds>(`${this.apiUrl}/fonds/${id}`);
  }

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

  cloturerPlacement(placementId: number): Observable<Placement> {
    return this.http.patch<Placement>(
      `${this.apiUrl}/placements/${placementId}/cloturer`,
      {}
    );
  }

  annulerPlacement(placementId: number): Observable<Placement> {
    return this.http.patch<Placement>(
      `${this.apiUrl}/placements/${placementId}/annuler`,
      {}
    );
  }
}