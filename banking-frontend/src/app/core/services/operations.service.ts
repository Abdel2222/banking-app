import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface Operation {
  id: number;
  type?: string;
  montant: number;
  date?: string;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class OperationsService {
  private http = inject(HttpClient);
  private readonly base = 'http://localhost:8084/api/operations';

  recent(numCompte: string, limit = 5): Observable<Operation[]> {
    const params = new HttpParams().set('numCompte', numCompte).set('limit', limit);
    return this.http.get<any>(`${this.base}/recent`, { params }).pipe(
      map(res => Array.isArray(res) ? res : (res?.data ?? []))
    );
  }

  deposit(numCompte: string, montant: number, description = 'DEPOT_DASH') {
    return this.http.post(`${this.base}/deposit`, { numCompte, montant, description });
  }

  withdraw(numCompte: string, montant: number, description = 'RETRAIT_DASH') {
    return this.http.post(`${this.base}/withdraw`, { numCompte, montant, description });
  }
}
