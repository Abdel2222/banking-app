import { Component, inject, signal, OnInit, OnDestroy, computed } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { HttpClient } from '@angular/common/http';

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
  imports: [CommonModule, ReactiveFormsModule, MoneyPipe, DatePipe, DecimalPipe, ChatbotComponent],
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
  private http = inject(HttpClient);

  private destroy$ = new Subject<void>();
  private readonly API_BASE = 'http://localhost:8084/api';
  private readonly TAUX_CONCURRENTIEL = 0.025;

  userName = signal<string | null>(null);
  error = signal<string | null>(null);
  busy = signal<boolean>(false);
  loading = signal<boolean>(true);
  operationsServiceDown = signal<boolean>(false);

  accounts = signal<BankAccount[]>([]);
  selected = signal<BankAccount | null>(null);
  card = signal<CardInfo | null>(null);
  ops = signal<Operation[]>([]);

  frais = signal<any[]>([]);
  savingsInfo = signal<any>(null);
  showFraisModal = signal<boolean>(false);

  notifications = this.notificationService.notifications;
  unreadCount = computed(() => this.notificationService.getUnreadCount());
  showNotifications = signal(false);

  fraisForm = this.fb.group({
    montant: [2.89, [Validators.required, Validators.min(0)]],
    description: ['Frais de gestion mensuel', Validators.required],
    typeFrais: ['TENUE_COMPTE', Validators.required],
    periodicite: ['MENSUEL', Validators.required]
  });

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
    const user = this.auth.currentUser();
    this.userName.set(user?.nomComplet || user?.email || 'Client');
    this.loadAccounts();
  }

  private loadAccounts() {
    this.loading.set(true);
    this.error.set(null);

    this.virementService.getCurrentUserComptes().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (comptes) => {
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

        this.accounts.set(normalizedAccounts);

        if (normalizedAccounts.length > 0) {
          const current = this.selected();
          const sameAccount = current
            ? normalizedAccounts.find(a => a.numCompte === current.numCompte)
            : null;

          this.select(sameAccount || normalizedAccounts[0]);
        } else {
          this.selected.set(null);
          this.card.set(null);
          this.ops.set([]);
        }

        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.handleApiError(e, 'Erreur de chargement des comptes');
      }
    });
  }

  private handleApiError(error: any, defaultMessage: string) {
    console.error('[DASHBOARD] API Error:', error);

    if (error.status === 401) {
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
    this.selected.set(account);
    this.error.set(null);
    this.loadCard(account.numCompte);
    this.loadOps(account.numCompte);
    this.loadFrais();
    this.loadSavings(account.numCompte);
  }

  private loadCard(numCompte: string) {
    this.cards.getByAccount(numCompte).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (cardData) => {
        this.card.set(cardData ?? null);
      },
      error: () => {
        this.card.set(null);
      }
    });
  }

  private loadOps(numCompte: string) {
    if (this.operationsServiceDown()) {
      this.operationsServiceDown.set(false);
    }

    this.opsApi.recent(numCompte, 5).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (rows) => {
        const operations = Array.isArray(rows) ? rows.map(r => this.normalizeOp(r)) : [];
        this.ops.set(operations);
      },
      error: (e) => {
        this.ops.set([]);
        if (e.status === 500) {
          this.operationsServiceDown.set(true);
        }
      }
    });
  }

  isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  isSelectedSuspended(): boolean {
    const status = this.selected()?.status;
    return status === 'SUSPENDED' || status === 'BLOCKED';
  }

  isAccountSuspended(account: BankAccount | null | undefined): boolean {
    const status = account?.status;
    return status === 'SUSPENDED' || status === 'BLOCKED';
  }

  getDisplayStatus(status: string | undefined): string {
    if (status === 'SUSPENDED' || status === 'BLOCKED') {
      return '⛔ SUSPENDED';
    }

    return status || 'N/A';
  }

  isCardBlocked(): boolean {
    return !!this.card() && !this.card()?.estActive;
  }

  private blockIfSuspended(action: 'virement' | 'depot' | 'retrait'): boolean {
    if (!this.isSelectedSuspended()) {
      return false;
    }

    if (action === 'virement') {
      alert('⛔ Ce compte est SUSPENDU. Vous ne pouvez pas effectuer de virement.');
    }

    if (action === 'depot') {
      alert('⛔ Les dépôts sont temporairement bloqués.');
    }

    if (action === 'retrait') {
      alert('⛔ Les retraits sont temporairement bloqués.');
    }

    return true;
  }

  private loadFrais() {
    const user: any = this.auth.currentUser();
    const clientId = user?.id;

    if (!clientId) {
      this.frais.set([]);
      return;
    }

    this.http.get<any>(`${this.API_BASE}/frais/client/${clientId}`).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (data) => {
        const fraisList = Array.isArray(data) ? data : (data?.data ?? []);
        this.frais.set(fraisList);
      },
      error: () => {
        this.frais.set([]);
      }
    });
  }
  facturerFraisManuellement() {
  if (!confirm('Déclencher la facturation de tous les frais actifs ?')) return;

  this.busy.set(true);

  // Appelle l'endpoint existant
  this.http.post<any>(
    `${this.API_BASE}/frais/fees/gestion/apply-all?montant=2.89&autoriserDecouvert=false`,
    {}
  ).pipe(
    takeUntil(this.destroy$)
  ).subscribe({
    next: (resp) => {
      this.busy.set(false);
      const stats = resp?.stats;
      alert(
        `✅ Facturation effectuée !\n\n` +
        `Total comptes : ${stats?.total || 0}\n` +
        `Frais appliqués : ${stats?.appliques || 0}\n` +
        `Solde insuffisant : ${stats?.soldeInsuffisant || 0}\n` +
        `Erreurs : ${stats?.erreurs || 0}`
      );
      this.refreshAccount();
      this.loadFrais();
    },
    error: (e) => {
      this.busy.set(false);
      this.error.set('Erreur facturation: ' + (e?.error?.message || e?.message || ''));
    }
  });
}

  private loadSavings(numCompte: string) {
    const sel = this.selected();
    const isEpargne = sel?.intitule?.toLowerCase().includes('épargne');

    if (!isEpargne) {
      const compteEpargne = this.accounts().find(a =>
        a.intitule?.toLowerCase().includes('épargne')
      );

      if (compteEpargne) {
        this.fetchSavingsData(compteEpargne.numCompte, compteEpargne.balance ?? 0);
      } else {
        this.savingsInfo.set(null);
      }

      return;
    }

    this.fetchSavingsData(numCompte, sel?.balance ?? 0);
  }

  private fetchSavingsData(numCompte: string, soldeCompte: number) {
    this.http.get<any>(`${this.API_BASE}/savings/${numCompte}/taxation`).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (data) => {
        this.applySavingsLogic(data, soldeCompte);
      },
      error: () => {
        this.applySavingsLogic(null, soldeCompte);
      }
    });
  }

  private applySavingsLogic(apiData: any, soldeCompte: number) {
    const soldeEpargne = soldeCompte > 0
      ? soldeCompte
      : (apiData?.soldeEpargne || 0);

    const tauxApi = apiData?.tauxInteret;
    const tauxInteret = (tauxApi && tauxApi > 0)
      ? tauxApi
      : this.TAUX_CONCURRENTIEL;

    const interetsAnnuels = soldeEpargne * tauxInteret;
    const interetsApi = apiData?.taxationVirtuelle;
    const interetsCumules = (interetsApi && interetsApi > 0)
      ? interetsApi
      : (interetsAnnuels / 12);

    this.savingsInfo.set({
      soldeEpargne,
      tauxInteret,
      taxationVirtuelle: interetsCumules,
      interetsAnnuelsEstimes: interetsAnnuels
    });
  }

  capitaliserInterets() {
    const num = this.selected()?.numCompte;
    if (!num) return;

    this.busy.set(true);

    this.http.post(`${this.API_BASE}/savings/${num}/capitaliser`, {}).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.busy.set(false);
        alert('✨ Intérêts capitalisés avec succès !');
        this.loadSavings(num);
        this.refreshAccount();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set('Erreur capitalisation: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  toggleSuspendCompte() {
    const sel = this.selected();
    if (!sel) return;

    const isSuspended = this.isAccountSuspended(sel);

    const confirmed = confirm(
      isSuspended
        ? 'Êtes-vous sûr de vouloir réactiver ce compte ?'
        : 'Êtes-vous sûr de vouloir suspendre ce compte ?'
    );

    if (!confirmed) return;

    this.busy.set(true);
    this.error.set(null);

    const request = isSuspended
      ? this.acc.activate(sel.numCompte)
      : this.acc.suspend(sel.numCompte);

    request.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.busy.set(false);
        alert(isSuspended ? '✅ Compte réactivé' : '⏸️ Compte suspendu');
        this.refreshAccount();
      },
      error: (e) => {
        this.busy.set(false);

        console.error('Erreur suspension/réactivation compte:', e);

        const message =
          e?.error?.message ||
          e?.error?.error ||
          e?.message ||
          `Erreur HTTP ${e?.status || 'inconnue'} - ${e?.statusText || 'Aucun détail'}`;

        this.error.set(message);
      }
    });
  }

  telechargerReleve() {
    const num = this.selected()?.numCompte;
    if (!num) return;

    this.busy.set(true);

    this.http.get(`${this.API_BASE}/comptes/${num}/releve`, {
      responseType: 'blob'
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (blob: Blob) => {
        this.busy.set(false);

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');

        a.href = url;
        a.download = `releve_${num}_${new Date().toISOString().split('T')[0]}.pdf`;

        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);

        window.URL.revokeObjectURL(url);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set('Erreur génération PDF: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  openFraisModal() {
    this.showFraisModal.set(true);
  }

  closeFraisModal() {
    this.showFraisModal.set(false);
  }

  submitFrais() {
    const user: any = this.auth.currentUser();
    const clientId = user?.id;

    if (!clientId || this.fraisForm.invalid) {
      this.error.set('Formulaire invalide');
      return;
    }

    const today = new Date();
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    const formatDate = (d: Date) => d.toISOString().split('T')[0];

    const body = {
      ...this.fraisForm.value,
      dateDebut: formatDate(today),
      dateFin: formatDate(lastDay),
      estActif: true
    };

    this.busy.set(true);

    this.http.post(`${this.API_BASE}/frais/client/${clientId}`, body).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.busy.set(false);
        this.closeFraisModal();
        this.loadFrais();
        alert('✅ Frais ajouté avec succès');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set('Erreur ajout frais: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  goToInvestments() {
    const selectedAccount = this.selected();

    if (!selectedAccount) {
      this.error.set('Veuillez sélectionner un compte');
      return;
    }

    if (this.isSelectedSuspended()) {
      alert('⛔ Ce compte est SUSPENDU. Vous ne pouvez pas investir dans un fonds.');
      return;
    }

    this.router.navigate(['/investissements'], {
      queryParams: {
        compteId: selectedAccount.id,
        numCompte: selectedAccount.numCompte
      }
    });
  }

  goToInvestmentsHistory() {
    this.router.navigate(['/investissements/historique']);
  }

  goToVirement() {
    const selectedAccount = this.selected();

    if (!selectedAccount) {
      this.error.set('Veuillez sélectionner un compte');
      return;
    }

    if (this.blockIfSuspended('virement')) {
      return;
    }

    this.router.navigate(['/virement'], {
      queryParams: { numCompte: selectedAccount.numCompte }
    });
  }

  goToOperations() {
    const selectedAccount = this.selected();

    if (selectedAccount) {
      this.router.navigate(['/operations'], {
        queryParams: { numCompte: selectedAccount.numCompte }
      });
    } else {
      this.router.navigate(['/operations']);
    }
  }

  goToCardDetails() {
    const selectedAccount = this.selected();

    if (selectedAccount && this.card()) {
      this.router.navigate(['/card-details'], {
        queryParams: { numCompte: selectedAccount.numCompte }
      });
    }
  }

  goToRDVDepot() {
    const selectedAccount = this.selected();

    if (!selectedAccount) {
      this.error.set('Veuillez sélectionner un compte');
      return;
    }

    if (this.blockIfSuspended('depot')) {
      return;
    }

    this.router.navigate(['/rendez-vous'], {
      queryParams: { type: 'DÉPÔT', numCompte: selectedAccount.numCompte }
    });
  }

  goToRDVRetrait() {
    const selectedAccount = this.selected();

    if (!selectedAccount) {
      this.error.set('Veuillez sélectionner un compte');
      return;
    }

    if (this.blockIfSuspended('retrait')) {
      return;
    }

    this.router.navigate(['/rendez-vous'], {
      queryParams: { type: 'RETRAIT', numCompte: selectedAccount.numCompte, minMontant: 3000 }
    });
  }

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
    if (!this.isAdmin()) {
      alert('⛔ Action réservée à l’administrateur.');
      return;
    }

    const selectedAccount = this.selected();
    const currentCard = this.card();

    if (!selectedAccount || !currentCard) {
      this.error.set('Aucune carte à gérer');
      return;
    }

    const confirmed = confirm(
      currentCard.estActive
        ? 'Êtes-vous sûr de vouloir BLOQUER cette carte ?'
        : 'Êtes-vous sûr de vouloir débloquer cette carte ?'
    );

    if (!confirmed) return;

    this.busy.set(true);
    this.error.set(null);

    const request = currentCard.estActive
      ? this.cards.bloquer(selectedAccount.numCompte, 'Blocage depuis interface')
      : this.cards.debloquer(selectedAccount.numCompte);

    request.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.busy.set(false);
        alert(currentCard.estActive ? '🔒 Carte bloquée' : '🔓 Carte débloquée');
        setTimeout(() => this.loadCard(selectedAccount.numCompte), 500);
      },
      error: (e) => {
        this.busy.set(false);
        this.handleApiError(e, 'Erreur gestion carte');
      }
    });
  }

  private refreshAccount() {
    const selectedAccount = this.selected();
    if (!selectedAccount) return;

    this.loadCard(selectedAccount.numCompte);
    this.loadOps(selectedAccount.numCompte);
    this.loadAccounts();
    this.loadFrais();
    this.loadSavings(selectedAccount.numCompte);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/auth']);
  }

  retryOperations() {
    this.operationsServiceDown.set(false);

    const selectedAccount = this.selected();

    if (selectedAccount) {
      this.loadOps(selectedAccount.numCompte);
    }
  }

  refreshData() {
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
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(balance || 0);
  }

  formatDate(date: string | null): string {
    if (!date) return 'N/A';

    try {
      return new Date(date).toLocaleDateString('fr-FR');
    } catch {
      return 'Date invalide';
    }
  }

  deleteNotification(id: number) {
    const updated = this.notifications().filter(n => n.id !== id);
    this.notificationService.notifications.set(updated);
  }
  // === SIGNAL pour le panneau frais ===
showFraisPanel = signal<boolean>(false);

// === Méthodes ===
toggleFraisPanel() {
  this.showFraisPanel.update(v => !v);
  // Ferme les notifs si ouvert
  if (this.showFraisPanel()) {
    this.showNotifications.set(false);
  }
}

scrollToFraisSection() {
  this.showFraisPanel.set(false);
  setTimeout(() => {
    const el = document.querySelector('.frais-section');
    el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, 100);
}


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
