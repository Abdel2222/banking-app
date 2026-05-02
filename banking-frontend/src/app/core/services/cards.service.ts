import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface CardInfo {
  id: number;
  numeroCarte?: string;
  dateExpiration?: string;
  estActive: boolean;
  plafondJournalier?: number;
  plafondMensuel?: number;
  compteId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class CardsService {
  private http = inject(HttpClient);
  private readonly base = 'http://localhost:8084/api/cartes';

  getByAccount(numCompte: string): Observable<CardInfo | null> {
    return this.http.get<any>(`${this.base}/${numCompte}`).pipe(
      map(res => (res && typeof res === 'object' && 'data' in res) ? res.data : res ?? null)
    );
  }

  issue(numCompte: string) {
    return this.http.post(`${this.base}/${numCompte}/issue`, {});
  }

  bloquer(numCompte: string, raison = 'dashboard') {
    return this.http.post(`${this.base}/${numCompte}/bloquer`, null, { params: { raison } as any });
  }

  debloquer(numCompte: string) {
    return this.http.post(`${this.base}/${numCompte}/debloquer`, {});
  }

  setPlafonds(numCompte: string, plafondJournalier: number, plafondMensuel: number) {
    return this.http.patch(`${this.base}/${numCompte}/plafonds`, { plafondJournalier, plafondMensuel });
  }
}
