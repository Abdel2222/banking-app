import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export type Intent =
  | 'BLOCK_CARD'
  | 'REPLACE_CARD'
  | 'PHONE_SUPPORT'
  | 'TECH_SUPPORT'
  | 'OTHER';

export interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  ts?: string;
}

export interface SupportRequest {
  intent: Intent;
  message?: string;
  numCompte?: string;
  numeroCarte?: string;
  phone?: string;
  email?: string;
}

@Injectable({ providedIn: 'root' })
export class SupportService {
  private http = inject(HttpClient);
  private base = 'http://localhost:8084/api/support'; // adapte si proxy

  // Envoie d’un message libre à un “bot” (optionnel si tu implémentes un bot côté back)
  chat(message: string): Observable<{ reply: string }> {
    return this.http.post<any>(`${this.base}/chat`, { message }).pipe(
      map(r => ({ reply: (r?.data?.reply ?? r?.reply ?? 'Merci, un conseiller vous répondra sous peu.') }))
    );
  }

  // Création d’un ticket d’assistance structuré
  request(body: SupportRequest): Observable<{ ticketId: string }> {
    return this.http.post<any>(`${this.base}/request`, body).pipe(
      map(r => ({ ticketId: (r?.data?.ticketId ?? r?.ticketId ?? 'TKT-' + Math.random().toString(36).slice(2, 8)) }))
    );
  }

  // Actions directes carte (facultatif si tu as déjà CardsService)
  blockCard(numeroCarteOrCompte: string) {
    return this.http.post(`${this.base}/card/${encodeURIComponent(numeroCarteOrCompte)}/block`, {});
  }

  replaceCard(numeroCarteOrCompte: string) {
    return this.http.post(`${this.base}/card/${encodeURIComponent(numeroCarteOrCompte)}/replace`, {});
  }
}
