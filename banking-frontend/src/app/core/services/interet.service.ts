import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Interet {
  id: number;
  dateDebut: string;          // ISO date "2026-04-19"
  dateFin: string | null;
  tauxInteret: number;        // ex: 0.03 = 3%
  montantInteret: number;
  dateCapitalisation: string | null;
}

export interface CalculInteret {
  numCompte: string;
  montantInterets: number;
}

@Injectable({ providedIn: 'root' })
export class InteretService {
  private http = inject(HttpClient);
  private readonly API_BASE = 'http://localhost:8084/api/interets';

  /** Créer un intérêt pour un compte épargne (admin) */
  creer(numCompte: string, taux: number): Observable<Interet> {
    return this.http.post<Interet>(
      `${this.API_BASE}/${numCompte}?taux=${taux}`,
      {}
    );
  }
  /** Appliquer un taux à TOUS les comptes épargne (admin) */
appliquerTauxATous(taux: number): Observable<any> {
  return this.http.post(`${this.API_BASE}/appliquer-tous`, null, {
    params: { taux: taux.toString() }
  });
}

/** Capitaliser TOUS les intérêts (admin) */
capitaliserTous(): Observable<any> {
  return this.http.post(`${this.API_BASE}/capitaliser-tous`, null, {});
}

  /** Calculer le montant des intérêts en cours (sans capitaliser) */
  calculer(numCompte: string): Observable<CalculInteret> {
    return this.http.get<CalculInteret>(`${this.API_BASE}/${numCompte}/calcul`);
  }

  /** Capitaliser : crédite le solde + clôture la période courante */
  capitaliser(numCompte: string): Observable<Interet> {
    return this.http.post<Interet>(
      `${this.API_BASE}/${numCompte}/capitaliser`,
      {}
    );
  }

  /** Récupérer l'historique des intérêts d'un compte */
  historique(numCompte: string): Observable<Interet[]> {
    return this.http.get<Interet[]>(`${this.API_BASE}/${numCompte}/historique`);
  }

  /** Modifier le taux courant */
  updateTaux(numCompte: string, taux: number): Observable<Interet> {
    return this.http.put<Interet>(
      `${this.API_BASE}/${numCompte}/taux?valeur=${taux}`,
      {}
    );
  }
}
