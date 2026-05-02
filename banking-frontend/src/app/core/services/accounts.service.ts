import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

export interface BankAccount {
  id: number;
  numCompte: string;
  balance?: number;
  solde?: number;
  status: 'PENDING' | 'ACTIVATED' | 'BLOCKED' | string;
  clientName?: string;
  hasCard?: boolean;
  savingsAccount?: boolean;
  createdAt?: string;
  intitule?: string;
}

@Injectable({ providedIn: 'root' })
export class AccountsService {
  private http = inject(HttpClient);
  private readonly base = '/api/comptes';

  list(): Observable<BankAccount[]> {
    return this.http.get<any>(this.base).pipe(
      map(res => Array.isArray(res) ? res : (res?.data ?? []))
    );
  }

  activate(numCompte: string) {
    return this.http.post(`${this.base}/${numCompte}/activer`, {});
  }
}
