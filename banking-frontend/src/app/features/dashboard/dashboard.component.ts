import { Component, inject, signal, OnInit, OnDestroy, computed } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil, interval } from 'rxjs';
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
  showFraisPanel = signal<boolean>(false);

  readonly visibleNotifications = computed(() =>
    this.notifications().filter((n: any) => n.type !== 'rdv')
  );

  readonly unreadVisibleNotificationsCount = computed(() =>
    this.notifications().filter((n: any) => !n.read && n.type !== 'rdv').length
  );

  fraisForm = this.fb.group({
    montant: [2.89, [Validators.required, Validators.min(0)]],
    description: ['Frais de gestion mensuel', Validators.required],
    typeFrais: ['TENUE_COMPTE', Validators.required],
    periodicite: ['MENSUEL', Validators.required]
  });

  ngOnInit() {
    this.accounts.set([]);
    this.selected.set(null);
    this.card.set(null);
    this.ops.set([]);
    this.frais.set([]);
    this.savingsInfo.set(null);
    this.notificationService.clearAll();

    this.loadData();

    interval(8000).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadChatbotResponses());
    interval(15000).pipe(takeUntil(this.destroy$)).subscribe(() => {
      const sel = this.selected();
      if (sel) this.loadCard(sel.numCompte);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadData() {
    const user = this.auth.currentUser();
    this.userName.set(user?.nomComplet || user?.email || 'Client');
    this.loadAccounts();
    this.loadChatbotResponses();
  }

  private loadChatbotResponses() {
    const clientId = this.getClientId();
    if (!clientId) return;
    this.http.get<any[]>(`${this.API_BASE}/chats/client/${clientId}/repondus`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (chats) => {
          if (!Array.isArray(chats)) return;
          for (const chat of chats) {
            this.notificationService.addChatbotResponseNotification(
              chat.id,
              chat.reponseAdmin || chat.reponse || 'Votre demande a ete traitee.',
              chat.dateReponse ? new Date(chat.dateReponse) : new Date()
            );
          }
        },
        error: (e) => console.error('[CHATBOT NOTIF]', e)
      });
  }

  private loadAccounts() {
    this.loading.set(true);
    this.error.set(null);
    this.virementService.getCurrentUserComptes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (comptes) => {
          const accounts: BankAccount[] = comptes.map((c: any) => ({
            id: c.id,
            numCompte: c.numCompte,
            balance: c.balance,
            status: c.status,
            intitule: c.intitule,
            devise: c.devise,
            clientName: undefined,
            hasCard: c.hasCard ?? false,
            savingsAccount: c.hasSavingsAccount === true || c.savingsAccount === true,
            createdAt: undefined
          }));
          this.accounts.set(accounts);
          if (accounts.length > 0) {
            const current = this.selected();
            const same = current ? accounts.find(a => a.numCompte === current.numCompte) : null;
            this.select(same || accounts[0]);
          } else {
            this.selected.set(null);
            this.card.set(null);
            this.ops.set([]);
          }
          this.loading.set(false);
        },
        error: (e) => { this.loading.set(false); this.handleApiError(e, 'Erreur chargement comptes'); }
      });
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
    this.cards.getByAccount(numCompte)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (c) => this.card.set(c ?? null),
        error: () => this.card.set(null)
      });
  }

  private loadOps(numCompte: string) {
    if (this.operationsServiceDown()) this.operationsServiceDown.set(false);
    this.opsApi.recent(numCompte, 5)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rows) => this.ops.set(Array.isArray(rows) ? rows.map(r => this.normalizeOp(r, numCompte)) : []),
        error: (e) => { this.ops.set([]); if (e.status === 500) this.operationsServiceDown.set(true); }
      });
  }

  // ✅ Fix signe virement + affichage communication
  private normalizeOp(o: any, numCompteActuel?: string): Operation {
    let montant = Number(o?.montant ?? 0);
    const typeOp = (o?.type ?? o?.typeOperation ?? '').toString().toUpperCase();
    const description = o?.description ?? o?.libelle ?? typeOp ?? 'Opération';

    // ✅ Virement sortant → montant négatif
    if (typeOp === 'VIREMENT' && montant > 0) {
      const numCompteOp = o?.numeroCompte ?? o?.numCompte ?? '';
      const numCompteDest = o?.numeroCompteDestinataire ?? '';
      // Si le numéro de compte de l'opération = compte actuel ET destinataire différent → débit
      if (numCompteOp === numCompteActuel && numCompteDest && numCompteDest !== numCompteActuel) {
        montant = -montant;
      }
      // Si la description contient "Réception" → crédit, sinon débit
      if (description.toLowerCase().includes('réception') || description.toLowerCase().includes('reception')) {
        montant = Math.abs(montant); // crédit
      } else if (!description.toLowerCase().includes('réception') && numCompteDest && numCompteDest !== numCompteActuel) {
        montant = -Math.abs(montant); // débit
      }
    }

    // ✅ Frais → toujours négatif
    const isFrais = description.toLowerCase().includes('frais') ||
      typeOp === 'FRAIS' || typeOp === 'FRAIS_GESTION' || typeOp === 'BLOCAGE_CARTE';
    if (isFrais && montant > 0) montant = -montant;

    // ✅ Communication = nom destinataire ou communication
    const contrepartie = o?.nomTitulaireDestinataire || o?.communication || null;
    const descFinale = contrepartie && !description.toLowerCase().includes(contrepartie.toLowerCase())
      ? `${description} — ${contrepartie}`
      : description;

    return {
      id: o?.id ?? 0,
      montant,
      date: o?.date ?? o?.dateOperation ?? o?.createdAt ?? null,
      description: descFinale
    };
  }

  private loadFrais() {
    const clientId = this.getClientId();
    if (!clientId) { this.frais.set([]); return; }
    this.http.get<any>(`${this.API_BASE}/frais/client/${clientId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (d) => this.frais.set(Array.isArray(d) ? d : (d?.data ?? [])),
        error: () => this.frais.set([])
      });
  }

  openFraisModal() { this.showFraisModal.set(true); }
  closeFraisModal() { this.showFraisModal.set(false); }

  submitFrais() {
    const clientId = this.getClientId();
    if (!clientId || this.fraisForm.invalid) { this.error.set('Formulaire invalide'); return; }
    const today = new Date();
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    const fmt = (d: Date) => d.toISOString().split('T')[0];
    const body = { ...this.fraisForm.value, dateDebut: fmt(today), dateFin: fmt(lastDay), estActif: true };
    this.busy.set(true);
    this.http.post(`${this.API_BASE}/frais/client/${clientId}`, body)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => { this.busy.set(false); this.closeFraisModal(); this.loadFrais(); alert('Frais ajoute'); },
        error: (e) => { this.busy.set(false); this.error.set('Erreur frais: ' + (e?.error?.message || '')); }
      });
  }

  deleteFrais(fraisId: number) {
    if (!confirm('Supprimer ce frais ?')) return;
    this.busy.set(true);
    this.http.delete(`${this.API_BASE}/frais/${fraisId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => { this.busy.set(false); alert('Frais supprime'); this.loadFrais(); },
        error: (e) => { this.busy.set(false); this.error.set('Erreur: ' + (e?.error?.message || '')); }
      });
  }

  private loadSavings(numCompte: string) {
    const sel = this.selected();
    const isEpargne = sel?.savingsAccount === true;
    if (!isEpargne) {
      const compteEpargne = this.accounts().find(a => a.savingsAccount === true);
      compteEpargne
        ? this.fetchSavingsData(compteEpargne.numCompte, compteEpargne.balance ?? 0)
        : this.savingsInfo.set(null);
      return;
    }
    this.fetchSavingsData(numCompte, sel?.balance ?? 0);
  }

  private fetchSavingsData(numCompte: string, solde: number) {
    this.http.get<any>(`${this.API_BASE}/savings/${numCompte}/taxation`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (d) => this.applySavingsLogic(d, solde),
        error: () => this.applySavingsLogic(null, solde)
      });
  }

  private applySavingsLogic(apiData: any, solde: number) {
    const soldeEpargne = solde > 0 ? solde : (apiData?.soldeEpargne || 0);
    const tauxApi = apiData?.tauxInteret;
    const tauxInteret = tauxApi && tauxApi > 0 ? tauxApi : this.TAUX_CONCURRENTIEL;
    const interetsAnnuels = soldeEpargne * tauxInteret;
    const interetsApi = apiData?.taxationVirtuelle;
    const interetsCumules = interetsApi && interetsApi > 0 ? interetsApi : interetsAnnuels / 12;
    this.savingsInfo.set({ soldeEpargne, tauxInteret, taxationVirtuelle: interetsCumules, interetsAnnuelsEstimes: interetsAnnuels });
  }

  capitaliserInterets() {
    const num = this.selected()?.numCompte;
    if (!num) return;
    this.busy.set(true);
    this.http.post(`${this.API_BASE}/savings/${num}/capitaliser`, {})
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => { this.busy.set(false); alert('Interets capitalises !'); this.loadSavings(num); this.refreshAccount(); },
        error: (e) => { this.busy.set(false); this.error.set('Erreur capitalisation: ' + (e?.error?.message || '')); }
      });
  }

  toggleSuspendCompte() {
    const sel = this.selected();
    if (!sel) return;
    const isSuspended = this.isAccountSuspended(sel);
    if (!confirm(isSuspended ? 'Reactiver ce compte ?' : 'Suspendre ce compte ?')) return;
    this.busy.set(true);
    this.error.set(null);
    const req = isSuspended ? this.acc.activate(sel.numCompte) : this.acc.suspend(sel.numCompte);
    req.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.busy.set(false); alert(isSuspended ? 'Compte reactive' : 'Compte suspendu'); this.refreshAccount(); },
      error: (e) => { this.busy.set(false); this.error.set(e?.error?.message || `HTTP ${e?.status}`); }
    });
  }

  telechargerReleve() {
    const num = this.selected()?.numCompte;
    if (!num) return;
    this.busy.set(true);
    this.http.get(`${this.API_BASE}/comptes/${num}/releve`, { responseType: 'blob' })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
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
        error: (e) => { this.busy.set(false); this.error.set('Erreur PDF: ' + (e?.error?.message || '')); }
      });
  }

  toggleCard() {
    if (!this.isAdmin()) { alert('Action reservee a l\'administrateur.'); return; }
    const sel = this.selected();
    const card = this.card();
    if (!sel || !card) { this.error.set('Aucune carte'); return; }
    if (!confirm(card.estActive ? 'BLOQUER cette carte ?' : 'Debloquer cette carte ?')) return;
    this.busy.set(true);
    this.error.set(null);
    const req = card.estActive
      ? this.cards.bloquer(sel.numCompte, 'Blocage interface')
      : this.cards.debloquer(sel.numCompte);
    req.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.busy.set(false); alert(card.estActive ? 'Carte bloquee' : 'Carte debloquee'); setTimeout(() => this.loadCard(sel.numCompte), 500); },
      error: (e) => { this.busy.set(false); this.handleApiError(e, 'Erreur carte'); }
    });
  }

  requestCardReplacement() {
    window.dispatchEvent(new CustomEvent('open-chatbot-with-message', {
      detail: { message: 'Remplacer ma carte' }
    }));
  }

  goToInvestments() {
    const sel = this.selected();
    if (!sel) { this.error.set('Selectionnez un compte'); return; }
    if (this.isSelectedPending()) { alert('⏳ Compte en attente d\'activation. Investissement bloqué.'); return; }
    if (this.isSelectedSuspended()) { alert('Compte SUSPENDU. Investissement bloque.'); return; }
    this.router.navigate(['/investissements'], { queryParams: { compteId: sel.id, numCompte: sel.numCompte } });
  }

  goToInvestmentsHistory() { this.router.navigate(['/investissements/historique']); }

  goToVirement() {
    const sel = this.selected();
    if (!sel) { this.error.set('Selectionnez un compte'); return; }
    if (this.blockIfNotReady('virement')) return;
    this.router.navigate(['/virement'], { queryParams: { numCompte: sel.numCompte } });
  }

  goToOperations() {
    const sel = this.selected();
    sel
      ? this.router.navigate(['/operations'], { queryParams: { numCompte: sel.numCompte } })
      : this.router.navigate(['/operations']);
  }

  goToCardDetails() {
    const sel = this.selected();
    if (sel && this.card()) {
      this.router.navigate(['/card-details'], { queryParams: { numCompte: sel.numCompte } });
    }
  }

  goToRDVDepot() {
    const sel = this.selected();
    if (!sel) { this.error.set('Selectionnez un compte'); return; }
    if (this.blockIfNotReady('depot')) return;
    this.router.navigate(['/rendez-vous'], { queryParams: { type: 'DEPOT', numCompte: sel.numCompte } });
  }

  goToRDVRetrait() {
    const sel = this.selected();
    if (!sel) { this.error.set('Selectionnez un compte'); return; }
    if (this.blockIfNotReady('retrait')) return;
    this.router.navigate(['/rendez-vous'], { queryParams: { type: 'RETRAIT', numCompte: sel.numCompte, minMontant: 3000 } });
  }

  goToSavings() {
    const clientId = this.getClientId();
    if (!clientId) { this.error.set('Impossible de recuperer l\'identifiant client'); return; }
    this.http.get<any>(`${this.API_BASE}/comptes/client/${clientId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          const comptes = Array.isArray(response) ? response : [response];
          const epargne =
            comptes.find((c: any) => c?.hasSavingsAccount === true || c?.savingsAccount === true) ||
            this.accounts().find(a => a.savingsAccount === true);
          if (!epargne) {
            if (!confirm('Pas de compte epargne. En creer un ?')) return;
            const base = this.selected() || this.accounts()[0];
            if (!base) { alert('Aucun compte'); return; }
            this.router.navigate(['/epargne'], { queryParams: { mode: 'create', from: base.numCompte } });
            return;
          }
          this.router.navigate(['/epargne'], { queryParams: { numCompte: epargne.numCompte } });
        },
        error: () => {
          const epargne = this.accounts().find(a => a.savingsAccount === true);
          epargne
            ? this.router.navigate(['/epargne'], { queryParams: { numCompte: epargne.numCompte } })
            : this.error.set('Aucun compte epargne');
        }
      });
  }

  toggleNotifications() {
    this.showNotifications.update(v => !v);
    if (this.showNotifications()) this.loadChatbotResponses();
  }

  markAsRead(id: number) { this.notificationService.markAsRead(id); }
  clearAllNotifications() { this.notificationService.clearAll(); this.showNotifications.set(false); }
  deleteNotification(id: number) { this.notificationService.deleteNotification(id); }
  toggleFraisPanel() { this.showFraisPanel.update(v => !v); if (this.showFraisPanel()) this.showNotifications.set(false); }

  isAdmin(): boolean { return this.auth.isAdmin(); }
  isSelectedSuspended(): boolean { return this.isAccountSuspended(this.selected()); }
  isAccountSuspended(a: BankAccount | null | undefined): boolean { return a?.status === 'SUSPENDED' || a?.status === 'BLOCKED'; }
  isSelectedPending(): boolean { const s = this.selected()?.status; return s === 'CREATED' || s === 'PENDING' || s === 'INACTIVE'; }
  isCardBlocked(): boolean { return !!this.card() && !this.card()?.estActive; }
  get canToggleCard(): boolean { return !!this.selected() && !!this.card() && !this.busy(); }
  hasVisibleNotifications(): boolean { return this.visibleNotifications().length > 0; }
  hasUnreadVisibleNotifications(): boolean { return this.unreadVisibleNotificationsCount() > 0; }

  private blockIfNotReady(action: 'virement' | 'depot' | 'retrait'): boolean {
    if (this.isSelectedPending()) {
      alert('⏳ Votre compte est en attente d\'activation par un conseiller. Cette action est temporairement indisponible.');
      return true;
    }
    if (!this.isSelectedSuspended()) return false;
    const msgs = { virement: 'Compte SUSPENDU. Virement bloque.', depot: 'Depot bloque.', retrait: 'Retrait bloque.' };
    alert(msgs[action]);
    return true;
  }

  private getClientId(): number | string | null {
    const id = this.auth.clientIdFromToken;
    if (id) return id;
    const user: any = this.auth.currentUser();
    const sub = user?.sub;
    if (sub) { const n = Number(sub); return isNaN(n) ? sub : n; }
    return null;
  }

  private handleApiError(error: any, msg: string) {
    if (error.status === 401) { this.auth.logout(); this.router.navigate(['/auth']); }
    else if (error.status === 404) { this.error.set('Aucune donnee trouvee'); }
    else if (error.status === 500) { this.error.set('Service indisponible'); if (msg.includes('operation')) this.operationsServiceDown.set(true); }
    else if (error.status === 0) { this.error.set('Serveur non accessible.'); }
    else { this.error.set(error?.error?.message || error?.message || msg); }
  }

  private refreshAccount() {
    const sel = this.selected();
    if (!sel) return;
    this.loadCard(sel.numCompte);
    this.loadOps(sel.numCompte);
    this.loadAccounts();
    this.loadFrais();
    this.loadSavings(sel.numCompte);
  }

  retryOperations() { this.operationsServiceDown.set(false); const sel = this.selected(); if (sel) this.loadOps(sel.numCompte); }
  refreshData() { this.error.set(null); this.loadAccounts(); }

  logout() {
    this.auth.logout();
    this.router.navigate(['/auth']).then(() => { window.location.reload(); });
  }

  formatCardExpiry(date: string | undefined): string {
    if (!date) return 'MM/AA';
    try {
      const d = new Date(date);
      return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getFullYear()).slice(-2)}`;
    } catch { return 'MM/AA'; }
  }

  formatBalance(balance: number | undefined): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(balance || 0);
  }

  displayName(): string {
    const u: any = this.auth.currentUser();
    if (!u) return 'Client';
    const nom =
      u.nomComplet || u.fullName || u.name ||
      [u.prenom, u.nom].filter(Boolean).join(' ') ||
      [u.firstName, u.lastName].filter(Boolean).join(' ') ||
      u.sub || u.email || 'Client';
    return (typeof nom === 'string' && nom.trim()) ? nom.trim() : 'Client';
  }
}