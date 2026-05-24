import { Component, signal, OnInit, OnDestroy, inject, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, interval } from 'rxjs';

import { ChatService, ChatMessage, ChatActionType, ChatStatut } from '../../core/services/chat.service';

type StatusFilter = 'EN_ATTENTE' | 'REPONDU' | 'REJETE' | 'ALL';

@Component({
  selector: 'app-admin-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './admin-chatbot.component.html',
  styleUrls: ['./admin-chatbot.component.scss']
})
export class AdminChatbotComponent implements OnInit, OnDestroy {
  private chatService = inject(ChatService);
  private destroy$ = new Subject<void>();

  // === Données ===
  allMessages = signal<ChatMessage[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  // === Filtres ===
  statusFilter = signal<StatusFilter>('EN_ATTENTE');
  actionFilter = signal<ChatActionType | 'ALL'>('ALL');
  searchQuery = signal<string>('');

  // === Sélection / réponse ===
  selected = signal<ChatMessage | null>(null);
  responseText = signal<string>('');
  rejectMotif = signal<string>('');
  showRejectDialog = signal<boolean>(false);
  busy = signal<boolean>(false);

  // === Stats calculées ===
  stats = computed(() => {
    const all = this.allMessages();
    return {
      total:      all.length,
      enAttente:  all.filter(m => m.statut === 'EN_ATTENTE').length,
      repondu:    all.filter(m => m.statut === 'REPONDU').length,
      rejete:     all.filter(m => m.statut === 'REJETE').length,
    };
  });

  // === Liste filtrée ===
  filteredMessages = computed(() => {
    let list = this.allMessages();

    const status = this.statusFilter();
    if (status !== 'ALL') {
      list = list.filter(m => m.statut === status);
    }

    const action = this.actionFilter();
    if (action !== 'ALL') {
      list = list.filter(m => m.actionType === action);
    }

    const q = this.searchQuery().trim().toLowerCase();
    if (q) {
      list = list.filter(m =>
        (m.clientNom || '').toLowerCase().includes(q) ||
        (m.numCompte || '').toLowerCase().includes(q) ||
        (m.contenu || '').toLowerCase().includes(q)
      );
    }

    // Tri : EN_ATTENTE en premier, puis par date desc
    return [...list].sort((a, b) => {
      if (a.statut === 'EN_ATTENTE' && b.statut !== 'EN_ATTENTE') return -1;
      if (a.statut !== 'EN_ATTENTE' && b.statut === 'EN_ATTENTE') return 1;
      return new Date(b.dateHeure).getTime() - new Date(a.dateHeure).getTime();
    });
  });

  ngOnInit() {
    this.loadMessages();

    // Polling toutes les 15s pour voir les nouvelles demandes
    interval(15000).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.loadMessages(true));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadMessages(silent = false) {
    if (!silent) this.loading.set(true);
    this.error.set(null);

    // On charge à la fois pending et l'historique récent
    this.chatService.getPending().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (pending) => {
        // On garde aussi les messages déjà traités qu'on a en mémoire
        const existingTreated = this.allMessages().filter(m => m.statut !== 'EN_ATTENTE');
        const pendingIds = new Set(pending.map(p => p.id));
        const merged = [
          ...pending,
          ...existingTreated.filter(m => !pendingIds.has(m.id))
        ];

        this.allMessages.set(merged);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set('Impossible de charger les demandes : ' + (e?.error?.message || e?.message || ''));
        this.loading.set(false);
      }
    });
  }

  selectMessage(msg: ChatMessage) {
    this.selected.set(msg);
    this.responseText.set(msg.reponseAdmin || '');
    this.rejectMotif.set('');
    this.showRejectDialog.set(false);
  }

  closeDetails() {
    this.selected.set(null);
    this.responseText.set('');
    this.showRejectDialog.set(false);
  }

  respond() {
    const msg = this.selected();
    const text = this.responseText().trim();
    if (!msg || !text) {
      alert('Veuillez écrire une réponse avant de l\'envoyer.');
      return;
    }

    this.busy.set(true);
    this.chatService.respond(msg.id, text).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.applyUpdate(updated);
        this.closeDetails();
        alert('✅ Réponse envoyée au client.');
      },
      error: (e) => {
        this.busy.set(false);
        alert('Erreur : ' + (e?.error?.message || e?.message || 'envoi impossible'));
      }
    });
  }

  markTreated() {
    const msg = this.selected();
    if (!msg) return;

    if (!confirm('Marquer cette demande comme traitée (sans réponse personnalisée) ?')) return;

    this.busy.set(true);
    this.chatService.markTreated(msg.id).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.applyUpdate(updated);
        this.closeDetails();
      },
      error: (e) => {
        this.busy.set(false);
        alert('Erreur : ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  openRejectDialog() {
    this.showRejectDialog.set(true);
  }

  cancelReject() {
    this.showRejectDialog.set(false);
    this.rejectMotif.set('');
  }

  confirmReject() {
    const msg = this.selected();
    const motif = this.rejectMotif().trim();
    if (!msg) return;
    if (!motif) {
      alert('Veuillez préciser un motif de refus.');
      return;
    }

    this.busy.set(true);
    this.chatService.reject(msg.id, motif).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.applyUpdate(updated);
        this.closeDetails();
      },
      error: (e) => {
        this.busy.set(false);
        alert('Erreur : ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  private applyUpdate(updated: ChatMessage) {
    this.allMessages.update(list => {
      const idx = list.findIndex(m => m.id === updated.id);
      if (idx === -1) return [updated, ...list];
      const copy = [...list];
      copy[idx] = updated;
      return copy;
    });
  }

  setStatusFilter(value: StatusFilter) {
    this.statusFilter.set(value);
  }

  onActionFilterChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value as ChatActionType | 'ALL';
    this.actionFilter.set(value);
  }

  onSearchChange(event: Event) {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  refresh() {
    this.loadMessages();
  }

  // === Helpers d'affichage ===

  actionLabel(action: ChatActionType): string {
    const labels: Record<ChatActionType, string> = {
      NORMAL:           'Message normal',
      BLOQUER_CARTE:    '🔒 Bloquer carte',
      DEBLOQUER_CARTE:  '🔓 Débloquer carte',
      PERTE_VOL_CARTE:  '🚨 Perte / Vol carte',
      SUSPENDRE_COMPTE: '⏸️ Suspendre compte',
      REACTIVER_COMPTE: '▶️ Réactiver compte',
      SIGNALER_FRAUDE:  '⚠️ Fraude signalée',
      RECLAMATION:      '📋 Réclamation',
      AUTRE_SENSIBLE:   '❓ Demande sensible',
    };
    return labels[action] || action;
  }

  actionClass(action: ChatActionType): string {
    const map: Record<ChatActionType, string> = {
      NORMAL:           'action-normal',
      BLOQUER_CARTE:    'action-card-block',
      DEBLOQUER_CARTE:  'action-card-unblock',
      PERTE_VOL_CARTE:  'action-card-lost',
      SUSPENDRE_COMPTE: 'action-account-suspend',
      REACTIVER_COMPTE: 'action-account-reactivate',
      SIGNALER_FRAUDE:  'action-fraud',
      RECLAMATION:      'action-complaint',
      AUTRE_SENSIBLE:   'action-other',
    };
    return map[action] || '';
  }

  statusLabel(statut: ChatStatut): string {
    const labels: Record<ChatStatut, string> = {
      NORMAL:     'Normal',
      EN_ATTENTE: '⏳ En attente',
      REPONDU:    '✅ Répondu',
      REJETE:     '❌ Refusé',
    };
    return labels[statut] || statut;
  }

  statusClass(statut: ChatStatut): string {
    return 'status-' + statut.toLowerCase().replace('_', '-');
  }
}
