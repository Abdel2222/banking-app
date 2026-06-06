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

  allMessages = signal<ChatMessage[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  statusFilter = signal<StatusFilter>('EN_ATTENTE');
  actionFilter = signal<ChatActionType | 'ALL'>('ALL');
  searchQuery = signal<string>('');

  selected = signal<ChatMessage | null>(null);
  responseText = signal<string>('');
  rejectMotif = signal<string>('');
  showRejectDialog = signal<boolean>(false);
  busy = signal<boolean>(false);

  stats = computed(() => {
    const all = this.allMessages();
    return {
      total:             all.length,
      enAttente:         all.filter(m => m.statut === 'EN_ATTENTE').length,
      repondu:           all.filter(m => m.statut === 'REPONDU').length,
      rejete:            all.filter(m => m.statut === 'REJETE').length,
      blocageCarte:      all.filter(m => m.actionType === 'BLOCAGE_CARTE').length,
      fraude:            all.filter(m => m.actionType === 'FRAUDE_CVV').length,
      deblocageCarte:    all.filter(m => m.actionType === 'DEBLOCAGE_CARTE').length,
      remplacementCarte: all.filter(m => m.actionType === 'REMPLACEMENT_CARTE').length,
      pbVirement:        all.filter(m => m.actionType === 'PROBLEME_VIREMENT').length,
      pbCompte:          all.filter(m => m.actionType === 'PROBLEME_COMPTE').length,
    };
  });

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

    return [...list].sort((a, b) => {
      const priorityA = this.getPriority(a);
      const priorityB = this.getPriority(b);
      if (priorityA !== priorityB) return priorityA - priorityB;
      return new Date(b.dateHeure).getTime() - new Date(a.dateHeure).getTime();
    });
  });

  private getPriority(msg: ChatMessage): number {
    if (msg.actionType === 'FRAUDE_CVV') return 0;
    if (msg.actionType === 'BLOCAGE_CARTE') return 1;
    if (msg.actionType === 'REMPLACEMENT_CARTE') return 2;
    if (msg.statut === 'EN_ATTENTE') return 3;
    return 4;
  }

  ngOnInit() {
    this.loadMessages();
    interval(15000).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadMessages(true));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadMessages(silent = false) {
    if (!silent) this.loading.set(true);
    this.error.set(null);

    this.chatService.getPending().pipe(takeUntil(this.destroy$)).subscribe({
      next: (pending) => {
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
    this.responseText.set(this.getSuggestedResponse(msg));
    this.rejectMotif.set('');
    this.showRejectDialog.set(false);
  }

  getSuggestedResponse(msg: ChatMessage): string {
    const nom = msg.clientNom ? msg.clientNom.split(' ')[0] : 'Client';
    switch (msg.actionType) {
      case 'BLOCAGE_CARTE':
        return `Bonjour ${nom}, votre carte bancaire a bien été bloquée temporairement. ` +
               `Contactez-nous pour la débloquer si nécessaire. ` +
               `Cordialement, T€chno-Bank.`;

      case 'REMPLACEMENT_CARTE':
        return `Bonjour ${nom}, votre ancienne carte a été bloquée et une nouvelle carte a été émise. ` +
               `Elle est disponible immédiatement dans votre espace client. ` +
               `Cordialement, T€chno-Bank.`;

      case 'FRAUDE_CVV':
        return `Bonjour ${nom}, votre signalement de fraude a été pris en charge. ` +
               `Votre carte a été sécurisée. Nous vous contacterons rapidement. ` +
               `Cordialement, T€chno-Bank.`;

      case 'DEBLOCAGE_CARTE':
        return `Bonjour ${nom}, votre carte bancaire a bien été débloquée. ` +
               `Elle est à nouveau active. ` +
               `Cordialement, T€chno-Bank.`;

      case 'PROBLEME_VIREMENT':
        return `Bonjour ${nom}, nous avons vérifié votre virement. ` +
               `Pouvez-vous nous préciser la date et le montant concerné ? ` +
               `Cordialement, T€chno-Bank.`;

      case 'PROBLEME_COMPTE':
        return `Bonjour ${nom}, nous avons examiné votre compte. ` +
               `N'hésitez pas à nous préciser la nature exacte du problème. ` +
               `Cordialement, T€chno-Bank.`;

      default:
        return msg.reponseAdmin || '';
    }
  }

  closeDetails() {
    this.selected.set(null);
    this.responseText.set('');
    this.showRejectDialog.set(false);
  }

  respond() {
    const msg = this.selected();
    const text = this.responseText().trim();
    if (!msg || !text) { alert('Veuillez écrire une réponse.'); return; }
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
        alert('Erreur : ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  markTreated() {
    const msg = this.selected();
    if (!msg) return;
    if (!confirm('Marquer comme traitée ?')) return;
    this.busy.set(true);
    this.chatService.markTreated(msg.id).subscribe({
      next: (updated) => { this.busy.set(false); this.applyUpdate(updated); this.closeDetails(); },
      error: (e) => { this.busy.set(false); alert('Erreur : ' + (e?.error?.message || '')); }
    });
  }

  openRejectDialog() { this.showRejectDialog.set(true); }
  cancelReject() { this.showRejectDialog.set(false); this.rejectMotif.set(''); }

  confirmReject() {
    const msg = this.selected();
    const motif = this.rejectMotif().trim();
    if (!msg) return;
    if (!motif) { alert('Veuillez préciser un motif.'); return; }
    this.busy.set(true);
    this.chatService.reject(msg.id, motif).subscribe({
      next: (updated) => { this.busy.set(false); this.applyUpdate(updated); this.closeDetails(); },
      error: (e) => { this.busy.set(false); alert('Erreur : ' + (e?.error?.message || '')); }
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

  setStatusFilter(value: StatusFilter) { this.statusFilter.set(value); }

  onActionFilterChange(event: Event) {
    this.actionFilter.set((event.target as HTMLSelectElement).value as ChatActionType | 'ALL');
  }

  onSearchChange(event: Event) {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  refresh() { this.loadMessages(); }

  actionLabel(action: ChatActionType): string {
    const labels: Record<ChatActionType, string> = {
      NORMAL:              '💬 Message normal',
      BLOCAGE_CARTE:       '🔒 Bloquer carte',
      FRAUDE_CVV:          '🚨 Fraude / CVV',
      DEBLOCAGE_CARTE:     '🔓 Débloquer carte',
      REMPLACEMENT_CARTE:  '💳 Remplacer carte',
      PROBLEME_VIREMENT:   '💸 Problème virement',
      PROBLEME_COMPTE:     '🏦 Problème compte',
    };
    return labels[action] || action;
  }

  actionClass(action: ChatActionType): string {
    const map: Record<ChatActionType, string> = {
      NORMAL:              'action-normal',
      BLOCAGE_CARTE:       'action-card-block',
      FRAUDE_CVV:          'action-fraud',
      DEBLOCAGE_CARTE:     'action-card-unblock',
      REMPLACEMENT_CARTE:  'action-card-replace',
      PROBLEME_VIREMENT:   'action-virement',
      PROBLEME_COMPTE:     'action-compte',
    };
    return map[action] || '';
  }

  actionInstruction(action: ChatActionType): string {
    switch (action) {
      case 'BLOCAGE_CARTE':
        return '👉 Aller dans Gestion Carte → Cliquer BLOQUER la carte du client';
      case 'REMPLACEMENT_CARTE':
        return '👉 Aller dans Gestion Carte → Cliquer BLOQUER puis Émettre une nouvelle carte';
      case 'FRAUDE_CVV':
        return '👉 Aller dans Gestion Carte → BLOQUER + régénérer CVV';
      case 'DEBLOCAGE_CARTE':
        return '👉 Aller dans Gestion Carte → Débloquer la carte du client';
      case 'PROBLEME_VIREMENT':
        return '👉 Vérifier les opérations du compte concerné';
      case 'PROBLEME_COMPTE':
        return '👉 Vérifier le statut du compte dans Gestion Comptes';
      default:
        return '';
    }
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