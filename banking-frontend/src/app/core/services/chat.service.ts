import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type ChatStatut = 'NORMAL' | 'EN_ATTENTE' | 'REPONDU' | 'REJETE';

export type ChatActionType =
  | 'NORMAL'
  | 'BLOQUER_CARTE'
  | 'DEBLOQUER_CARTE'
  | 'PERTE_VOL_CARTE'
  | 'SUSPENDRE_COMPTE'
  | 'REACTIVER_COMPTE'
  | 'SIGNALER_FRAUDE'
  | 'RECLAMATION'
  | 'AUTRE_SENSIBLE';

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

  // ===== CLIENT =====

  /** Crée un message côté client (avec auto-détection d'intention côté backend) */
  create(payload: string | CreateChatPayload): Observable<ChatMessage> {
    const body: CreateChatPayload = typeof payload === 'string'
      ? { contenu: payload }
      : payload;
    return this.http.post<ChatMessage>(this.API_BASE, body);
  }

  /** Récupère l'historique d'un client (pour voir les réponses admin) */
  getByClient(clientId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API_BASE}/client/${clientId}`);
  }

  // ===== ADMIN =====

  /** Liste des demandes en attente */
  getPending(): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API_BASE}/admin/pending`);
  }

  /** Compteur des demandes en attente */
  countPending(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.API_BASE}/admin/pending/count`);
  }

  /** Répondre à une demande */
  respond(id: number, message: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(
      `${this.API_BASE}/admin/${id}/respond`,
      { message }
    );
  }

  /** Marquer traité sans message particulier */
  markTreated(id: number): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.API_BASE}/admin/${id}/mark-treated`, {});
  }

  /** Refuser la demande avec un motif */
  reject(id: number, motif: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(
      `${this.API_BASE}/admin/${id}/reject`,
      { message: motif }
    );
  }
}
