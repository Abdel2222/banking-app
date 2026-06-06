import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type ChatStatut = 'NORMAL' | 'EN_ATTENTE' | 'REPONDU' | 'REJETE';

// ✅ Aligné avec ChatActionType.java du backend
export type ChatActionType =
  | 'NORMAL'
  | 'BLOCAGE_CARTE'
  | 'FRAUDE_CVV'
  | 'DEBLOCAGE_CARTE'
  | 'REMPLACEMENT_CARTE'
  | 'PROBLEME_VIREMENT'
  | 'PROBLEME_COMPTE';

export interface ChatMessage {
  id: number;
  contenu: string;
  dateHeure: string;
  reponse?: string;
  reponseAdmin?: string;
  statut: ChatStatut;
  actionType: ChatActionType;
  clientId?: number;
  clientNom?: string;
  numCompte?: string;
  adminId?: number;
  dateReponse?: string;
}

export interface CreateChatPayload {
  contenu: string;
  clientId?: number | null;
  clientNom?: string | null;
  numCompte?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private readonly API_BASE = 'http://localhost:8084/api/chats';

  create(payload: string | CreateChatPayload): Observable<ChatMessage> {
    const body: CreateChatPayload = typeof payload === 'string'
      ? { contenu: payload }
      : payload;
    return this.http.post<ChatMessage>(this.API_BASE, body);
  }

  getByClient(clientId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API_BASE}/client/${clientId}`);
  }

  getRepondusByClient(clientId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API_BASE}/client/${clientId}/repondus`);
  }

  getPending(): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API_BASE}/admin/pending`);
  }

  countPending(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.API_BASE}/admin/pending/count`);
  }

  respond(id: number, message: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(
      `${this.API_BASE}/admin/${id}/respond`,
      { message }
    );
  }

  markTreated(id: number): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.API_BASE}/admin/${id}/mark-treated`, {});
  }

  reject(id: number, motif: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(
      `${this.API_BASE}/admin/${id}/reject`,
      { message: motif }
    );
  }
}