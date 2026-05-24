
import { Injectable, signal, effect } from '@angular/core';

const KEY = 'banking.selectedAccount';

@Injectable({ providedIn: 'root' })
export class SelectedAccountService {
  readonly compteId = signal<number | null>(null);
  readonly numCompte = signal<string | null>(null);

  constructor() {
    const raw = sessionStorage.getItem(KEY);
    if (raw) {
      try {
        const { compteId, numCompte } = JSON.parse(raw);
        this.compteId.set(compteId ?? null);
        this.numCompte.set(numCompte ?? null);
      } catch {}
    }

    effect(() => {
      sessionStorage.setItem(KEY, JSON.stringify({
        compteId: this.compteId(),
        numCompte: this.numCompte(),
      }));
    });
  }

  set(compteId: number | null, numCompte: string | null) {
    this.compteId.set(compteId);
    this.numCompte.set(numCompte);
  }

  clear() {
    this.compteId.set(null);
    this.numCompte.set(null);
    sessionStorage.removeItem(KEY);
  }
}
