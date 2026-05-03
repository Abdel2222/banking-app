import { Component, inject, signal, OnInit, OnDestroy, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { AccountsService, BankAccount } from '../../core/services/accounts.service';
import { OperationsService, Operation } from '../../core/services/operations.service';
import { CardsService, CardInfo } from '../../core/services/cards.service';
import { VirementService } from '../../core/services/virement.service';
import { NotificationService } from '../../core/services/notification.service';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { ChatbotComponent } from '../chatbot/chatbot.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MoneyPipe, DatePipe, ChatbotComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private auth = inject(AuthService);
  private acc = inject(AccountsService);
  private opsApi = inject(OperationsService);
  private cards = inject(CardsService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private virementService = inject(VirementService);
  private notificationService = inject(NotificationService);

  private destroy$ = new Subject<void>();

  userName = signal<string | null>(null);
  error = signal<string | null>(null);
  busy = signal<boolean>(false);
  loading = signal<boolean>(true);
  operationsServiceDown = signal<boolean>(false);

  accounts = signal<BankAccount[]>([]);
  selected = signal<BankAccount | null>(null);
  card = signal<CardInfo | null>(null);
  ops = signal<Operation[]>([]);

  // NOTIFICATIONS
  notifications = this.notificationService.notifications;
  unreadCount = computed(() => this.notificationService.getUnreadCount());
  showNotifications = signal(false);

  constructor() {
    console.log('[DASHBOARD] Component initialized');
  }

  ngOnInit() {
    console.log('[DASHBOARD] ngOnInit - user authenticated by guard');
    this.loadData();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadData() {
    console.log('[DASHBOARD] Loading dashboard data...');
    const user = this.auth.currentUser();
    this.userName.set(user?.nomComplet || user?.email || 'Client');
    console.log('[DASHBOARD] User loaded from token:', user?.email);
    this.loadAccounts();
  }

  private loadAccounts() {
    console.log('[DASHBOARD] Loading accounts with VirementService...');
    this.loading.set(true);
    this.error.set(null);

    this.virementService.getCurrentUserComptes().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (comptes) => {
        console.log('[DASHBOARD] Comptes from VirementService:', comptes);

        const normalizedAccounts: BankAccount[] = comptes.map((c: any) => ({
          id: c.id,
          numCompte: c.numCompte,
          balance: c.balance,
          status: c.status,
          intitule: c.intitule,
          devise: c.devise,
          clientName: undefined,
          hasCard: false,
          savingsAccount: c.intitule?.toLowerCase().includes('épargne') || false,
          createdAt: undefined
        }));

        console.log('[DASHBOARD] Normalized accounts:', normalizedAccounts);
        this.accounts.set(normalizedAccounts);

        if (normalizedAccounts.length > 0) {
          console.log('[DASHBOARD] Auto-selecting first account');
          this.select(normalizedAccounts[0]);
        } else {
          console.log('[DASHBOARD] No accounts found');
          this.selected.set(null);
          this.card.set(null);
          this.ops.set([]);
        }

        this.loading.set(false);
      },
      error: (e) => {
        console.error('[DASHBOARD] Error loading accounts:', e);
        this.loading.set(false);
        this.handleApiError(e, 'Erreur de chargement des comptes');
      }
    });
  }

  private handleApiError(error: any, defaultMessage: string) {
    console.error('[DASHBOARD] API Error:', error);

    if (error.status === 401) {
      console.log('[DASHBOARD] Token expired, logging out');
      this.auth.logout();
      this.router.navigate(['/auth']);
    } else if (error.status === 404) {
      this.error.set('Aucune donnée trouvée');
    } else if (error.status === 500) {
      this.error.set('Service temporairement indisponible');
      if (defaultMessage.includes('opération')) {
        this.operationsServiceDown.set(true);
      }
    } else if (error.status === 0) {
      this.error.set('Serveur non accessible. Vérifiez la connexion.');
    } else {
      const errorMsg = error?.error?.message || error?.message || defaultMessage;
      this.error.set(errorMsg);
    }
  }

  private normalizeOp(o: any): Operation {
    return {
      id: o?.id ?? 0,
      montant: Number(o?.montant ?? 0),
      date: o?.date ?? o?.dateOperation ?? o?.createdAt ?? null,
      description: o?.description ?? o?.libelle ?? o?.type ?? 'Opération'
    };
  }

  select(account: BankAccount) {
    console.log('[DASHBOARD] Selecting account:', account.numCompte);
    this.selected.set(account);
    this.error.set(null);
    this.loadCard(account.numCompte);
    this.loadOps(account.numCompte);
  }

  private loadCard(numCompte: string) {
    console.log('[DASHBOARD] Loading card for account:', numCompte);
    this.cards.getByAccount(numCompte).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (cardData) => {
        console.log('[DASHBOARD] Card data received:', cardData);
        this.card.set(cardData ?? null);
      },
      error: (e) => {
        console.log('[DASHBOARD] No card found or error for account', numCompte, ':', e);
        this.card.set(null);
      }
    });
  }

  private loadOps(numCompte: string) {
    console.log('[DASHBOARD] Loading operations for account:', numCompte);
    if (this.operationsServiceDown()) {
      this.operationsServiceDown.set(false);
    }

    this.opsApi.recent(numCompte, 5).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (rows) => {
        console.log('[DASHBOARD] Operations data received:', rows);
        const operations = Array.isArray(rows) ? rows.map(r => this.normalizeOp(r)) : [];
        this.ops.set(operations);
      },
      error: (e) => {
        console.error('[DASHBOARD] Operations loading failed:', e);
        this.ops.set([]);
        if (e.status === 500) {
          this.operationsServiceDown.set(true);
          console.log('[DASHBOARD] Operations service appears to be down');
        }
      }
    });
  }

  goToVirement() {
    this.router.navigate(['/virement']);
  }
  goToOperations() {
  const selectedAccount = this.selected();
  if (selectedAccount) {
    console.log('[DASHBOARD] Navigating to operations for account:', selectedAccount.numCompte);
    this.router.navigate(['/operations'], {
      queryParams: { numCompte: selectedAccount.numCompte }
    });
  } else {
    console.log('[DASHBOARD] Navigating to operations without account selection');
    this.router.navigate(['/operations']);
  }
}

  goToCardDetails() {
    const selectedAccount = this.selected();
    if (selectedAccount && this.card()) {
      console.log('[DASHBOARD] Navigating to card details for account:', selectedAccount.numCompte);
      this.router.navigate(['/card-details'], {
        queryParams: { numCompte: selectedAccount.numCompte }
      });
    }
  }

  // MÉTHODE RDV DÉPÔT
  goToRDVDepot() {
    const selectedAccount = this.selected();
    if (!selectedAccount) {
      this.error.set('Veuillez sélectionner un compte');
      return;
    }

    console.log('[DASHBOARD] Redirection vers RDV pour dépôt');

    this.router.navigate(['/rendez-vous'], {
      queryParams: {
        type: 'DÉPÔT',
        numCompte: selectedAccount.numCompte
      }
    });
  }

  // MÉTHODE RDV RETRAIT (minimum 3000€)
  goToRDVRetrait() {
    const selectedAccount = this.selected();
    if (!selectedAccount) {
      this.error.set('Veuillez sélectionner un compte');
      return;
    }

    const MIN_RETRAIT = 3000;

    console.log('[DASHBOARD] Redirection vers RDV pour retrait (minimum', MIN_RETRAIT, '€)');

    this.router.navigate(['/rendez-vous'], {
      queryParams: {
        type: 'RETRAIT',
        numCompte: selectedAccount.numCompte,
        minMontant: MIN_RETRAIT
      }
    });
  }

  // MÉTHODES NOTIFICATIONS
  toggleNotifications() {
    this.showNotifications.update(v => !v);
  }

  markAsRead(id: number) {
    this.notificationService.markAsRead(id);
  }

  clearAllNotifications() {
    this.notificationService.clearAll();
    this.showNotifications.set(false);
  }

  toggleCard() {
    const selectedAccount = this.selected();
    const currentCard = this.card();

    if (!selectedAccount || !currentCard) {
      this.error.set('Aucune carte à gérer');
      return;
    }

    this.busy.set(true);
    this.error.set(null);

    const action = currentCard.estActive ? 'bloquer' : 'débloquer';
    console.log('[DASHBOARD] Card action:', action, 'for account', selectedAccount.numCompte);

    const request = currentCard.estActive
      ? this.cards.bloquer(selectedAccount.numCompte, 'Blocage depuis interface')
      : this.cards.debloquer(selectedAccount.numCompte);

    request.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (response) => {
        console.log('[DASHBOARD] Card toggle successful:', response);
        this.busy.set(false);
        setTimeout(() => {
          this.loadCard(selectedAccount.numCompte);
        }, 500);
      },
      error: (e) => {
        console.error('[DASHBOARD] Card toggle error:', e);
        this.busy.set(false);
        this.handleApiError(e, 'Erreur gestion carte');
      }
    });
  }

  private refreshAccount() {
    console.log('[DASHBOARD] Refreshing account data');
    const selectedAccount = this.selected();
    if (!selectedAccount) return;

    this.loadCard(selectedAccount.numCompte);
    this.loadOps(selectedAccount.numCompte);
    this.loadAccounts();
  }

  logout() {
    console.log('[DASHBOARD] User logging out');
    this.auth.logout();
    this.router.navigate(['/auth']);
  }

  retryOperations() {
    console.log('[DASHBOARD] Retrying operations service');
    this.operationsServiceDown.set(false);
    const selectedAccount = this.selected();
    if (selectedAccount) {
      this.loadOps(selectedAccount.numCompte);
    }
  }

  refreshData() {
    console.log('[DASHBOARD] Manual refresh requested');
    this.error.set(null);
    this.loadAccounts();
  }

  formatCardExpiry(date: string | undefined): string {
    if (!date) return 'MM/AA';
    try {
      const d = new Date(date);
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const year = String(d.getFullYear()).slice(-2);
      return `${month}/${year}`;
    } catch {
      return 'MM/AA';
    }
  }

  get canToggleCard(): boolean {
    return !!this.selected() && !!this.card() && !this.busy();
  }

  formatBalance(balance: number | undefined): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(balance || 0);
  }

  formatDate(date: string | null): string {
    if (!date) return 'N/A';
    try {
      return new Date(date).toLocaleDateString('fr-FR');
    } catch {
      return 'Date invalide';
    }
  }
  // Dans la classe DashboardComponent
deleteNotification(id: number) {
  const updated = this.notifications().filter(n => n.id !== id);
  this.notificationService.notifications.set(updated);
}
// Récupère le nom complet en testant toutes les clés du JWT
displayName(): string {
  const u: any = this.auth.currentUser();
  if (!u) return 'Client';

  const nom =
    u.nomComplet ||
    u.fullName ||
    u.name ||
    [u.prenom, u.nom].filter(Boolean).join(' ') ||
    [u.firstName, u.lastName].filter(Boolean).join(' ') ||
    u.sub ||
    u.email ||
    'Client';

  return (typeof nom === 'string' && nom.trim()) ? nom.trim() : 'Client';
}
}
