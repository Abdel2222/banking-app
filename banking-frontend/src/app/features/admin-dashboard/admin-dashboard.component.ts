import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';

import { AuthService } from '../../core/services/auth.service';
import { AccountsService, BankAccount } from '../../core/services/accounts.service';
import { OperationsService, Operation } from '../../core/services/operations.service';
import { CardsService, CardInfo } from '../../core/services/cards.service';
import { VirementService, Client, Compte } from '../../core/services/virement.service';
import { InteretService, Interet } from '../../core/services/interet.service';
import { SavingsService, CompteEpargne } from '../../core/services/savings.service';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { AdminChatbotComponent } from '../admin-chatbot/admin-chatbot.component';
import { InvestmentService, Fonds, Placement } from '../investments/investment.service';

interface CompteWithCard extends Compte {
  card?: CardInfo | null;
}

interface ClientWithAccounts extends Client {
  comptes: CompteWithCard[];
  placements?: Placement[];
}

interface AdminAccount extends BankAccount {
  clientId?: number;
  clientName?: string;
  cardNumber?: string;
  cardActive?: boolean;
  savingsAccount?: boolean;
  capitalisation?: boolean;
}

interface Employe {
  id: number;
  prenom: string;
  nom: string;
  email: string;
  role: string;
  matricule?: string;
  poste?: string;
  actif?: boolean;
}

interface AdminAgencyServiceNotification {
  id: number;
  date: string;
  heure: string;
  typeOperation: string;
  agence: string;
  montant: number;
  statut: 'PLANIFIE' | 'TERMINE' | 'ANNULE';
  clientNom?: string;
  numeroCompte?: string;
  guichet?: string;
  ticket?: string;
  conseiller?: string;
  conseillerMatricule?: string;
}

interface AccountSection {
  account: AdminAccount;
  card: CardInfo | null;
  ops: Operation[];
  frais: any[];
  epargneDetails?: CompteEpargne | null;
  interetCourant?: Interet | null;
  historiqueInterets?: Interet[];
  interetsEnCours?: number;
  totalInteretsCumules?: number;
  prochaineCapi?: Date | null;
  joursAvantCapi?: number;
  tauxAnnuel?: number;
}

type AdminTab =
  | 'accounts'
  | 'cards'
  | 'chatbot'
  | 'clients'
  | 'employees'
  | 'agencyServices'
  | 'investments'
  | 'funds';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, MoneyPipe, DatePipe, DecimalPipe, AdminChatbotComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);
  private acc = inject(AccountsService);
  private opsApi = inject(OperationsService);
  private cards = inject(CardsService);
  private fb = inject(FormBuilder);
  private virementService = inject(VirementService);
  private http = inject(HttpClient);
  private investmentService = inject(InvestmentService);
  private interetSvc = inject(InteretService);
  private savingsSvc = inject(SavingsService);

  private readonly API_BASE = 'http://localhost:8084/api';
  private loadedTabs = new Set<AdminTab>();
  private preloadingStarted = false;

  busy = signal<boolean>(false);
  me = signal<any>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  activeTab = signal<AdminTab>('accounts');

  accounts = signal<AdminAccount[]>([]);
  selected = signal<AdminAccount | null>(null);
  card = signal<CardInfo | null>(null);
  ops = signal<Operation[]>([]);

  selectedClientId = signal<number | null>(null);
  accountSections = signal<AccountSection[]>([]);
  loadingSections = signal<boolean>(false);

  frais = signal<any[]>([]);
  showFraisModal = signal<boolean>(false);
  fraisTargetAccountId = signal<number | null>(null);

  selectedCourant = signal<AdminAccount | null>(null);
  courantCard = signal<CardInfo | null>(null);

  // ✅ Barre de recherche pour Gestion Comptes
  accountsSearch = signal<string>('');
  // ✅ Barre de recherche pour Gestion Carte
  cardsSearch = signal<string>('');

  fraisForm = this.fb.group({
    montant: [2.89, [Validators.required, Validators.min(0.01)]],
    description: ['Frais de tenue de compte', Validators.required],
    typeFrais: ['TENUE_COMPTE', Validators.required],
    periodicite: ['MENSUELLE', Validators.required]
  });

  clients = signal<Client[]>([]);
  clientsWithAccounts = signal<ClientWithAccounts[]>([]);
  loadingClients = signal<boolean>(false);
  errorClients = signal<string | null>(null);
  searchFilter = signal<string>('');
  totalClients = signal<number>(0);
  expandedClientId = signal<number | null>(null);
  loadingPlacementsClientId = signal<number | null>(null);

  employees = signal<Employe[]>([]);
  loadingEmployees = signal<boolean>(false);
  errorEmployees = signal<string | null>(null);
  employeeSearch = signal<string>('');
  selectedEmployee = signal<Employe | null>(null);

  agencyServices = signal<AdminAgencyServiceNotification[]>([]);
  loadingAgencyServices = signal<boolean>(false);
  errorAgencyServices = signal<string | null>(null);

  investments = signal<Placement[]>([]);
  loadingInvestments = signal<boolean>(false);
  errorInvestments = signal<string | null>(null);

  fonds = signal<Fonds[]>([]);
  loadingFunds = signal<boolean>(false);
  errorFunds = signal<string | null>(null);

  selectedClientFunds = signal<Placement[]>([]);
  selectedClientName = signal<string>('');

  limitsForm = this.fb.group({
    plafondJournalier: [250, [Validators.required, Validators.min(1)]],
    plafondMensuel: [1000, [Validators.required, Validators.min(1)]],
  });

  tauxInteretParCompte: Record<string, number> = {};

  // ✅ Comptes filtrés par recherche (Gestion Comptes)
  readonly filteredAccounts = computed(() => {
    const q = this.accountsSearch().toLowerCase().trim();
    if (!q) return this.accounts();
    return this.accounts().filter(a =>
      (a.clientName || '').toLowerCase().includes(q) ||
      (a.numCompte || '').toLowerCase().includes(q) ||
      (a.status || '').toLowerCase().includes(q)
    );
  });

  // ✅ Comptes courants filtrés par recherche (Gestion Carte)
  readonly filteredComptesCourants = computed(() => {
    const q = this.cardsSearch().toLowerCase().trim();
    const courants = this.accounts().filter(a => !this.isEpargne(a));
    if (!q) return courants;
    return courants.filter(a =>
      (a.clientName || '').toLowerCase().includes(q) ||
      (a.numCompte || '').toLowerCase().includes(q) ||
      (a.status || '').toLowerCase().includes(q)
    );
  });

  constructor() {
    const token = localStorage.getItem('auth_token');
    if (!token || !this.auth.isAdmin()) {
      window.location.replace('/auth');
      return;
    }
    this.me.set(this.auth.currentUser());
  }

  ngOnInit() {
    setTimeout(async () => {
      const token = localStorage.getItem('auth_token');
      if (!token || !this.auth.isAdmin()) {
        window.location.replace('/auth');
        return;
      }
      await this.loadPrimaryDashboardData();
      this.preloadSecondaryTabs();
    }, 100);
  }

  async loadPrimaryDashboardData() {
    await this.loadAll(true);
    this.loadedTabs.add('accounts');
    this.loadedTabs.add('clients');
    this.loadedTabs.add('cards');
  }

  preloadSecondaryTabs() {
    if (this.preloadingStarted) return;
    this.preloadingStarted = true;
    setTimeout(() => {
      Promise.allSettled([
        this.loadEmployees(false),
        this.loadInvestments(false),
        this.loadFunds(false),
        this.loadAgencyServices(false)
      ]).then(() => {
        this.loadedTabs.add('employees');
        this.loadedTabs.add('investments');
        this.loadedTabs.add('funds');
        this.loadedTabs.add('agencyServices');
      });
    }, 300);
  }

  switchTab(tab: AdminTab) {
    this.activeTab.set(tab);
    if (this.loadedTabs.has(tab)) return;
    switch (tab) {
      case 'accounts':
      case 'clients':
      case 'cards':
        this.loadAll(true).then(() => {
          this.loadedTabs.add('accounts');
          this.loadedTabs.add('clients');
          this.loadedTabs.add('cards');
        });
        break;
      case 'employees':
        this.loadEmployees(true).then(() => this.loadedTabs.add('employees'));
        break;
      case 'agencyServices':
        this.loadAgencyServices(true).then(() => this.loadedTabs.add('agencyServices'));
        break;
      case 'investments':
        this.loadInvestments(true).then(() => this.loadedTabs.add('investments'));
        break;
      case 'funds':
        this.loadFunds(true).then(() => this.loadedTabs.add('funds'));
        break;
      case 'chatbot':
        this.loadedTabs.add('chatbot');
        break;
    }
  }

  async loadAll(showLoader = true): Promise<void> {
    const token = localStorage.getItem('auth_token');
    if (!token || !this.auth.isAdmin()) {
      this.error.set('Session expirée.');
      setTimeout(() => window.location.replace('/auth'), 500);
      return;
    }
    if (showLoader) { this.loading.set(true); this.loadingClients.set(true); }
    this.error.set(null);
    this.errorClients.set(null);
    try {
      const response: any = await firstValueFrom(this.virementService.getAllClients());
      const clients: Client[] = Array.isArray(response) ? response : (response?.data ?? []);
      this.clients.set(clients);
      this.totalClients.set(clients.length);
      if (clients.length === 0) {
        this.accounts.set([]);
        this.clientsWithAccounts.set([]);
        this.selected.set(null);
        this.card.set(null);
        this.ops.set([]);
        this.accountSections.set([]);
        return;
      }
      await this.loadClientsAccountsAndCards(clients);
    } catch {
      this.error.set('Impossible de charger les clients');
      this.errorClients.set('Impossible de charger les clients');
    } finally {
      if (showLoader) { this.loading.set(false); this.loadingClients.set(false); }
    }
  }

  getTauxParDefaut(numCompte: string): number {
    return this.tauxInteretParCompte[numCompte] ?? 0.03;
  }

  setTauxPourCompte(numCompte: string, value: number | string) {
    const taux = Number(value);
    this.tauxInteretParCompte[numCompte] = !isNaN(taux) && taux > 0 ? taux : 0.03;
  }

  creerInteretCompte(numCompte: string) {
    if (!numCompte) return;
    const taux = this.getTauxParDefaut(numCompte);
    if (!confirm(`Créer un intérêt actif pour ${numCompte} avec ${(taux * 100).toFixed(2)} % ?`)) return;
    this.busy.set(true);
    this.error.set(null);
    this.interetSvc.creer(numCompte, taux).subscribe({
      next: () => {
        this.busy.set(false);
        alert('✅ Intérêt créé');
        const clientId = this.selectedClientId();
        if (clientId) this.loadAccountSectionsForClient(clientId);
        this.loadAll(false);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set('Erreur: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  private async loadClientsAccountsAndCards(clients: Client[]) {
    const previousSelectedNum = this.selected()?.numCompte;
    const results = await Promise.all(
      clients.map(async (client) => {
        let comptes: Compte[] = [];
        try { comptes = await firstValueFrom(this.virementService.getComptesClient(client.id)); } catch { comptes = []; }
        return { client, comptes: comptes.map(c => ({ ...c, card: null })) };
      })
    );

    const clientsResults: ClientWithAccounts[] = [];
    const flatAccounts: AdminAccount[] = [];

    for (const item of results) {
      clientsResults.push({ ...item.client, comptes: item.comptes });
      for (const compte of item.comptes) {
        const isSavings = this.detectSavings(compte);
        flatAccounts.push({
          id: compte.id, numCompte: compte.numCompte, balance: compte.balance, solde: compte.balance,
          status: compte.status, intitule: compte.intitule, devise: compte.devise,
          clientId: item.client.id, clientName: `${item.client.prenom} ${item.client.nom}`,
          hasCard: compte.hasCard ?? false, cardNumber: undefined, cardActive: undefined,
          savingsAccount: isSavings, capitalisation: isSavings
        });
      }
    }

    this.clientsWithAccounts.set(clientsResults);
    this.accounts.set(flatAccounts);

    if (flatAccounts.length > 0) {
      const selectedAgain = previousSelectedNum ? flatAccounts.find(a => a.numCompte === previousSelectedNum) : null;
      this.select(selectedAgain || flatAccounts[0]);
    } else {
      this.selected.set(null); this.card.set(null); this.ops.set([]);
      this.frais.set([]); this.accountSections.set([]); this.selectedClientId.set(null);
    }
  }

  private detectSavings(compte: any): boolean {
    return !!compte.savingsAccount ||
      compte.intitule?.toLowerCase().includes('épargne') ||
      compte.intitule?.toLowerCase().includes('epargne');
  }

  select(a: AdminAccount) {
    const scrollY = window.scrollY;
    this.selected.set(a);
    this.error.set(null);
    if (a.clientId) {
      this.selectedClientId.set(a.clientId);
      this.loadAccountSectionsForClient(a.clientId);
    }
    this.loadSelectedAccountDetails(a.numCompte);
    setTimeout(() => window.scrollTo({ top: scrollY, behavior: 'instant' }), 50);
  }

  selectCompteForCard(account: AdminAccount) {
    this.selectedCourant.set(account);
    this.error.set(null);
    this.loadCourantCard(account.numCompte);
  }

  private loadCourantCard(numCompte: string) {
    this.cards.getByAccount(numCompte).subscribe({
      next: (c) => this.courantCard.set(c ?? null),
      error: () => this.courantCard.set(null)
    });
  }

  toggleCardForCourant() {
    const account = this.selectedCourant();
    const card = this.courantCard();
    if (!account || !card) return;
    const msg = card.estActive
      ? 'BLOQUER cette carte ?\n\n⚠️ Un nouveau CVV sera généré automatiquement.'
      : 'Débloquer cette carte ?';
    if (!confirm(msg)) return;
    this.loading.set(true);
    this.error.set(null);
    const bloc$ = card.estActive ? this.cards.bloquer(account.numCompte, 'admin') : this.cards.debloquer(account.numCompte);
    bloc$.pipe(
      switchMap(() => {
        if (card.estActive) {
          return this.http.post<any>(`${this.API_BASE}/cartes/${account.numCompte}/regenerate-cvv`, {})
            .pipe(catchError(() => of(null)));
        }
        return of(null);
      })
    ).subscribe({
      next: (resp) => {
        this.loading.set(false);
        if (card.estActive) {
          const newCvv = resp?.cvv ?? null;
          alert(newCvv
            ? `🔒 Carte bloquée\n\n🔑 Nouveau CVV : ${newCvv}\n\nCommuniquez ce code au client de façon sécurisée.`
            : '🔒 Carte bloquée et nouveau CVV généré.');
        } else {
          alert('🔓 Carte débloquée avec succès.');
        }
        this.loadCourantCard(account.numCompte);
        // ✅ Mettre à jour le statut carte dans la liste Gestion Comptes aussi
        const clientId = this.selectedClientId();
        if (clientId) this.loadAccountSectionsForClient(clientId);
      },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message || e?.message || 'Erreur'); }
    });
  }

  issueCardForCourant() {
    const account = this.selectedCourant();
    if (!account) return;
    if (!confirm('Émettre une carte pour ce compte courant ?')) return;
    this.loading.set(true);
    this.error.set(null);
    this.cards.issue(account.numCompte).subscribe({
      next: (resp: any) => {
        this.loading.set(false);
        const cvv = resp?.cvv ?? resp?.data?.cvv ?? null;
        alert(cvv
          ? `💳 Carte émise !\n\n🔑 CVV : ${cvv}\n\n⚠️ Ce code ne sera plus jamais affiché. Communiquez-le au client.`
          : '💳 Carte émise avec succès');
        this.loadCourantCard(account.numCompte);
        this.accounts.update(list => list.map(a => a.numCompte === account.numCompte ? { ...a, hasCard: true } : a));
      },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message || 'Émission carte KO'); }
    });
  }

  setLimitsForCourant() {
    const account = this.selectedCourant();
    if (!account) return;
    const j = Number(this.limitsForm.value.plafondJournalier);
    const m = Number(this.limitsForm.value.plafondMensuel);
    if (!j || !m) return;
    this.loading.set(true);
    this.error.set(null);
    this.cards.setPlafonds(account.numCompte, j, m).subscribe({
      next: () => { this.loading.set(false); alert('✅ Plafonds mis à jour'); this.loadCourantCard(account.numCompte); },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message || 'Maj plafonds KO'); }
    });
  }

  getComptesCourants(): AdminAccount[] {
    return this.accounts().filter(a => !this.isEpargne(a));
  }

  private async loadAccountSectionsForClient(clientId: number) {
    this.loadingSections.set(true);
    const clientAccounts = this.accounts().filter(a => a.clientId === clientId);
    const sections: AccountSection[] = await Promise.all(
      clientAccounts.map(async (account) => {
        const section: AccountSection = { account, card: null, ops: [], frais: [] };
        const tasks: Promise<any>[] = [
          this.loadFraisForClient(clientId).then(f => {
            section.frais = (f || []).filter((fr: any) =>
              !fr.compteBancaireId || fr.compteBancaireId === account.id || fr.compteBancaire?.id === account.id
            );
          })
        ];
        if (!account.savingsAccount) {
          tasks.push(
            this.loadCardForAccount(account.numCompte).then(c => { section.card = c; }),
            this.loadOpsForAccount(account.numCompte).then(o => { section.ops = o; })
          );
        }
        if (account.savingsAccount) {
          tasks.push(this.loadSavingsDetails(account.numCompte, section));
        }
        await Promise.allSettled(tasks);
        return section;
      })
    );
    this.accountSections.set(sections);
    this.loadingSections.set(false);
  }

  private async loadCardForAccount(num: string): Promise<CardInfo | null> {
    try { return await firstValueFrom(this.cards.getByAccount(num)) ?? null; } catch { return null; }
  }

  private async loadOpsForAccount(num: string): Promise<Operation[]> {
    try {
      const rows = await firstValueFrom(this.opsApi.recent(num, 20));
      return (rows ?? []).map(r => this.normalizeOp(r));
    } catch { return []; }
  }

  private async loadFraisForClient(clientId: number): Promise<any[]> {
    try {
      const resp: any = await firstValueFrom(this.http.get<any>(`${this.API_BASE}/frais/client/${clientId}`));
      return resp?.data ?? (Array.isArray(resp) ? resp : []);
    } catch { return []; }
  }

  private async loadSavingsDetails(num: string, section: AccountSection): Promise<void> {
    try {
      const [details, historique, calcul] = await Promise.allSettled([
        firstValueFrom(this.savingsSvc.getDetails(num)),
        firstValueFrom(this.interetSvc.historique(num)),
        firstValueFrom(this.interetSvc.calculer(num))
      ]);
      section.epargneDetails = details.status === 'fulfilled' ? details.value : null;
      section.historiqueInterets = historique.status === 'fulfilled' ? (historique.value || []) : [];
      section.interetsEnCours = calcul.status === 'fulfilled' ? Number(calcul.value?.montantInterets || 0) : 0;
      const sorted = [...(section.historiqueInterets || [])].sort((a, b) =>
        new Date(b.dateDebut).getTime() - new Date(a.dateDebut).getTime()
      );
      const interetActif = sorted.find(i => !i.dateCapitalisation) || null;
      section.interetCourant = interetActif;
      section.tauxAnnuel = interetActif ? Number(interetActif.tauxInteret) : 0;
      section.totalInteretsCumules = (section.historiqueInterets || [])
        .filter(i => !!i.dateCapitalisation)
        .reduce((sum, i) => sum + Number(i.montantInteret || 0), 0);
      if (interetActif) {
        const base = new Date(interetActif.dateDebut);
        const next = new Date(base);
        next.setMonth(next.getMonth() + 1);
        section.prochaineCapi = next;
        section.joursAvantCapi = Math.max(0, Math.ceil((next.getTime() - Date.now()) / 86400000));
      } else {
        section.prochaineCapi = null;
        section.joursAvantCapi = 0;
      }
    } catch (e) { console.error('loadSavingsDetails KO', e); }
  }

  async loadSelectedAccountDetails(numCompte: string) {
    await Promise.allSettled([
      this.loadCardAsync(numCompte),
      this.loadOpsAsync(numCompte),
      this.loadFraisForSelectedAsync()
    ]);
  }

  openClientAccount(compte: CompteWithCard, client: ClientWithAccounts) {
    const isSavings = this.detectSavings(compte);
    this.select({
      id: compte.id, numCompte: compte.numCompte, balance: compte.balance, solde: compte.balance,
      status: compte.status, intitule: compte.intitule, devise: compte.devise,
      clientId: client.id, clientName: `${client.prenom} ${client.nom}`,
      hasCard: compte.hasCard ?? false, cardNumber: compte.card?.numeroCarte,
      cardActive: compte.card?.estActive, savingsAccount: isSavings, capitalisation: isSavings
    });
    this.switchTab('accounts');
  }

  openClientCard(compte: CompteWithCard, client: ClientWithAccounts) {
    if (this.detectSavings(compte)) return;
    this.selectCompteForCard({
      id: compte.id, numCompte: compte.numCompte, balance: compte.balance, solde: compte.balance,
      status: compte.status, intitule: compte.intitule, devise: compte.devise,
      clientId: client.id, clientName: `${client.prenom} ${client.nom}`,
      hasCard: compte.hasCard ?? false, cardNumber: compte.card?.numeroCarte,
      cardActive: compte.card?.estActive, savingsAccount: false, capitalisation: false
    });
    this.switchTab('cards');
  }

  private async loadCardAsync(num: string): Promise<void> {
    try {
      const c = await firstValueFrom(this.cards.getByAccount(num));
      this.card.set(c ?? null);
      if (this.selected()?.numCompte === num && c)
        this.selected.update(s => s ? { ...s, hasCard: true, cardNumber: c.numeroCarte, cardActive: c.estActive } : s);
    } catch { this.card.set(null); }
  }

  private normalizeOp(o: any): Operation {
    let montant = Number(o?.montant ?? 0);
    const description = o?.description ?? o?.libelle ?? o?.type ?? 'Opération';
    const commentaire = o?.commentaire ?? '';
    const rawType = (o?.type ?? '').toString().toUpperCase();
    const isFrais = description.toLowerCase().includes('frais') ||
      commentaire.toUpperCase().includes('FRAIS_GESTION') ||
      rawType === 'FRAIS' || rawType === 'FRAIS_GESTION';
    if (isFrais && montant > 0) montant = -montant;
    return { id: o?.id ?? 0, montant, date: o?.date ?? o?.dateOperation ?? o?.createdAt ?? null, description };
  }

  private async loadOpsAsync(num: string): Promise<void> {
    try {
      const rows = await firstValueFrom(this.opsApi.recent(num, 20));
      this.ops.set((rows ?? []).map(r => this.normalizeOp(r)));
    } catch { this.ops.set([]); }
  }

  private async loadFraisForSelectedAsync(): Promise<void> {
    const clientId = this.selected()?.clientId;
    if (!clientId) { this.frais.set([]); return; }
    try {
      const resp: any = await firstValueFrom(this.http.get<any>(`${this.API_BASE}/frais/client/${clientId}`));
      this.frais.set(resp?.data ?? (Array.isArray(resp) ? resp : []));
    } catch { this.frais.set([]); }
  }

  openFraisModal(accountId?: number) {
    this.fraisTargetAccountId.set(accountId || this.selected()?.id || null);
    this.showFraisModal.set(true);
  }

  closeFraisModal() {
    this.showFraisModal.set(false);
    this.fraisTargetAccountId.set(null);
  }

  submitFrais(): void {
    const selected = this.selected();
    const clientId = selected?.clientId;
    const compteBancaireId = this.fraisTargetAccountId() || selected?.id;
    if (!selected || !clientId || !compteBancaireId) { this.error.set('Aucun compte valide.'); return; }
    if (this.fraisForm.invalid) { this.error.set('Formulaire invalide.'); this.fraisForm.markAllAsTouched(); return; }

    const today = new Date();
    const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    const fmt = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;

    const body = {
      montant: this.fraisForm.value.montant,
      description: this.fraisForm.value.description,
      typeFrais: this.fraisForm.value.typeFrais,
      periodicite: this.fraisForm.value.periodicite,
      dateDebut: fmt(today),
      dateFin: fmt(endOfMonth),
      estActif: true,
      compteBancaireId
    };

    this.loading.set(true);
    this.error.set('');
    this.http.post(`${this.API_BASE}/frais/client/${clientId}`, body).subscribe({
      next: () => {
        this.loading.set(false);
        this.closeFraisModal();
        this.fraisForm.reset({ montant: 2.89, description: 'Frais de tenue de compte', typeFrais: 'TENUE_COMPTE', periodicite: 'MENSUELLE' });
        this.loadFraisForSelectedAsync();
        if (clientId) this.loadAccountSectionsForClient(clientId);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set('Erreur: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  supprimerFrais(fraisId: number) {
    if (!confirm('Supprimer ce frais ?')) return;
    this.loading.set(true);

    // ✅ Suppression locale immédiate dans accountSections
    this.accountSections.update(sections =>
      sections.map(s => ({
        ...s,
        frais: s.frais.filter((f: any) => f.id !== fraisId)
      }))
    );

    this.http.delete(`${this.API_BASE}/frais/${fraisId}`).subscribe({
      next: () => {
        this.loading.set(false);
        // Recharger pour confirmer
        this.loadFraisForSelectedAsync();
        const clientId = this.selectedClientId();
        if (clientId) this.loadAccountSectionsForClient(clientId);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set('Erreur suppression: ' + (e?.error?.message || e?.message || ''));
        // Restaurer si erreur
        const clientId = this.selectedClientId();
        if (clientId) this.loadAccountSectionsForClient(clientId);
      }
    });
  }

  facturerSurCompte(numCompte: string) {
    if (!numCompte) return;
    const montantStr = prompt('Montant à débiter (€) ?', '2.89');
    if (!montantStr) return;
    const montant = parseFloat(montantStr.replace(',', '.'));
    if (isNaN(montant) || montant <= 0) { alert('Montant invalide'); return; }
    if (!confirm(`Débiter ${montant} € de frais sur ce compte ?`)) return;
    this.loading.set(true);
    this.http.post<any>(`${this.API_BASE}/frais/fees/gestion/${numCompte}?montant=${montant}&autoriserDecouvert=false`, {}).subscribe({
      next: (resp) => {
        this.loading.set(false);
        if (resp?.success) alert(`✅ Frais débité !\nAvant: ${resp.before} €\nAprès: ${resp.after} €`);
        else alert('⚠️ ' + (resp?.message || 'Échec'));
        this.refreshSelected();
      },
      error: (e) => { this.loading.set(false); this.error.set('Erreur: ' + (e?.error?.message || e?.message || '')); }
    });
  }

  facturerTousLesComptes() {
    const montantStr = prompt('Montant à débiter sur TOUS les comptes (€) ?', '2.89');
    if (!montantStr) return;
    const montant = parseFloat(montantStr.replace(',', '.'));
    if (isNaN(montant) || montant <= 0) { alert('Montant invalide'); return; }
    if (!confirm(`Débiter ${montant} € sur TOUS les comptes actifs ?`)) return;
    this.loading.set(true);
    this.http.post<any>(`${this.API_BASE}/frais/fees/gestion/apply-all?montant=${montant}&autoriserDecouvert=false`, {}).subscribe({
      next: (resp) => {
        this.loading.set(false);
        const s = resp?.stats;
        alert(`✅ Facturation effectuée !\nTotal: ${s?.total||0} | Appliqués: ${s?.appliques||0} | Insuff.: ${s?.soldeInsuffisant||0} | Erreurs: ${s?.erreurs||0}`);
        this.refreshSelected();
      },
      error: (e) => { this.loading.set(false); this.error.set('Erreur: ' + (e?.error?.message || e?.message || '')); }
    });
  }

  // ✅ Capitalisation possible 1 mois après le début de la période actuelle
  peutCapitaliser(section: AccountSection): boolean {
    if (!section.interetCourant) return false;
    const dateDebut = new Date(section.interetCourant.dateDebut);
    const unMoisApres = new Date(dateDebut);
    unMoisApres.setMonth(unMoisApres.getMonth() + 1);
    return new Date() >= unMoisApres;
  }

  // ✅ Message avec date cohérente avec "Prochaine capitalisation"
  getMessageCapitalisation(section: AccountSection): string {
    if (!section.interetCourant) return '✨ Capitaliser maintenant';
    const dateDebut = new Date(section.interetCourant.dateDebut);
    const unMoisApres = new Date(dateDebut);
    unMoisApres.setMonth(unMoisApres.getMonth() + 1);
    const fmt = unMoisApres.toLocaleDateString('fr-FR');
    return this.peutCapitaliser(section)
      ? '✨ Capitaliser maintenant'
      : `🔒 Disponible le ${fmt}`;
  }

  capitaliserCompte(numCompte: string) {
    if (!numCompte) return;
    if (!confirm('⚡ Capitaliser les intérêts maintenant ?')) return;
    this.busy.set(true);
    this.error.set(null);
    this.interetSvc.capitaliser(numCompte).subscribe({
      next: () => {
        this.busy.set(false);
        alert('✨ Intérêts capitalisés !');
        const clientId = this.selectedClientId();
        if (clientId) this.loadAccountSectionsForClient(clientId);
        this.loadAll(false);
      },
      error: (e) => { this.busy.set(false); this.error.set('Erreur: ' + (e?.error?.message || e?.message || '')); }
    });
  }

  async loadEmployees(showLoader = true): Promise<void> {
    if (showLoader) this.loadingEmployees.set(true);
    this.errorEmployees.set(null);
    try {
      const resp = await firstValueFrom(this.http.get<Employe[]>(`${this.API_BASE}/employes`));
      this.employees.set(Array.isArray(resp) ? resp : []);
    } catch { this.errorEmployees.set('Impossible de charger les employés'); }
    finally { if (showLoader) this.loadingEmployees.set(false); }
  }

  async loadAgencyServices(showLoader = true): Promise<void> {
    if (showLoader) this.loadingAgencyServices.set(true);
    this.errorAgencyServices.set(null);
    try {
      if (this.accounts().length === 0) await this.loadAll(false);
      let idCounter = 1;
      const allAgencyOps: AdminAgencyServiceNotification[] = [];
      const results = await Promise.all(
        this.accounts().map(async (acc) => {
          try {
            const ops = await firstValueFrom(this.opsApi.recent(acc.numCompte, 100));
            return (ops || [])
              .filter((op: any) => (op.description || op.libelle || '').toLowerCase().includes('en agence'))
              .map((op: any) => this.parseAgencyOp(op, acc, idCounter++));
          } catch { return []; }
        })
      );
      results.forEach(arr => allAgencyOps.push(...arr));
      allAgencyOps.sort((a, b) => {
        const p = (d: string, h: string) => { const [day,month,year] = d.split('/'); return new Date(`${year}-${month}-${day}T${h}:00`).getTime(); };
        return p(b.date, b.heure) - p(a.date, a.heure);
      });
      this.agencyServices.set(allAgencyOps);
    } catch { this.errorAgencyServices.set('Impossible de charger les opérations en agence'); }
    finally { if (showLoader) this.loadingAgencyServices.set(false); }
  }

  private parseAgencyOp(op: any, account: AdminAccount, id: number): AdminAgencyServiceNotification {
    const desc = op.description || op.libelle || '';
    const typeMatch = desc.match(/^(DÉPÔT|RETRAIT|DEPOT)/i);
    const guichetMatch = desc.match(/Guichet\s+(\d+)/i);
    const ticketMatch = desc.match(/Ticket\s+([A-Z0-9]+)/i);
    const conseillerMatch = desc.match(/Conseiller\s*:\s*([^(]+?)(?:\s*\(([^)]+)\))?$/i);
    const dateObj = new Date(op.date || op.dateOperation || op.createdAt || Date.now());
    return {
      id, date: dateObj.toLocaleDateString('fr-FR'),
      heure: dateObj.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }),
      typeOperation: typeMatch ? typeMatch[1].toUpperCase() : 'OPÉRATION',
      agence: 'Bruxelles Centre', montant: Math.abs(Number(op.montant || 0)), statut: 'TERMINE',
      clientNom: account.clientName, numeroCompte: account.numCompte,
      guichet: guichetMatch ? guichetMatch[1] : '—',
      ticket: ticketMatch ? ticketMatch[1] : '—',
      conseiller: conseillerMatch ? conseillerMatch[1].trim() : '—',
      conseillerMatricule: (conseillerMatch && conseillerMatch[2]) ? conseillerMatch[2].trim() : ''
    };
  }

  async loadInvestments(showLoader = true): Promise<void> {
    if (showLoader) this.loadingInvestments.set(true);
    this.errorInvestments.set(null);
    try {
      const resp = await firstValueFrom(this.investmentService.getTousLesPlacements());
      this.investments.set(resp || []);
    } catch { this.errorInvestments.set('Impossible de charger les investissements'); }
    finally { if (showLoader) this.loadingInvestments.set(false); }
  }

  async loadFunds(showLoader = true): Promise<void> {
    if (showLoader) this.loadingFunds.set(true);
    this.errorFunds.set(null);
    try {
      const resp = await firstValueFrom(this.investmentService.getTousLesFonds());
      this.fonds.set(resp || []);
    } catch { this.errorFunds.set('Impossible de charger les fonds'); }
    finally { if (showLoader) this.loadingFunds.set(false); }
  }

  showClientFunds(clientId: number, clientName: string) {
    this.loadingInvestments.set(true);
    this.selectedClientName.set(clientName);
    this.investmentService.getPlacementsByClient(clientId).subscribe({
      next: (resp) => { this.selectedClientFunds.set(resp || []); this.loadingInvestments.set(false); this.activeTab.set('funds'); },
      error: () => { this.selectedClientFunds.set([]); this.errorInvestments.set('Erreur placements'); this.loadingInvestments.set(false); }
    });
  }

  loadPlacementsForExpandedClient(clientId: number) {
    this.loadingPlacementsClientId.set(clientId);
    this.investmentService.getPlacementsByClient(clientId).subscribe({
      next: (resp) => {
        this.clientsWithAccounts.update(clients => clients.map(c => c.id === clientId ? { ...c, placements: resp || [] } : c));
        this.loadingPlacementsClientId.set(null);
      },
      error: () => {
        this.clientsWithAccounts.update(clients => clients.map(c => c.id === clientId ? { ...c, placements: [] } : c));
        this.loadingPlacementsClientId.set(null);
      }
    });
  }

  clearSelectedClientFunds() { this.selectedClientFunds.set([]); this.selectedClientName.set(''); }

  refreshSelected() {
    this.loadedTabs.delete('accounts');
    this.loadedTabs.delete('clients');
    this.loadedTabs.delete('cards');
    this.loadAll(true).then(() => {
      this.loadedTabs.add('accounts');
      this.loadedTabs.add('clients');
      this.loadedTabs.add('cards');
    });
  }

  isSelectedSuspended(): boolean { return this.isAccountSuspended(this.selected()); }

  isAccountSuspended(account: BankAccount | null | undefined): boolean {
    const status = account?.status;
    return status === 'SUSPENDED' || status === 'BLOCKED';
  }

  getDisplayStatus(status: string | undefined): string {
    if (status === 'SUSPENDED' || status === 'BLOCKED') return '⛔ SUSPENDED';
    return status || 'N/A';
  }

  getCardDisplay(account: AdminAccount): string {
    if (account.cardNumber) {
      const last4 = account.cardNumber.slice(-4);
      return account.cardActive === false ? `•••• ${last4} Bloquée` : `•••• ${last4} Active`;
    }
    if (account.hasCard) return 'Carte liée';
    return 'Aucune carte';
  }

  isEpargne(account: any): boolean {
    const intitule = (account?.intitule || '').toLowerCase();
    return !!account?.savingsAccount || !!account?.capitalisation ||
      intitule.includes('épargne') || intitule.includes('epargne');
  }

  getAccountDisplayType(account: any): string { return this.isEpargne(account) ? 'Épargne' : 'Courant'; }
  getAccountCategory(account: any): string { return this.isEpargne(account) ? 'Capitalisation' : 'Standard'; }

  getAccountTypeLabel(account: { intitule?: string; savingsAccount?: boolean }): string {
    if (account?.savingsAccount) return 'Épargne';
    const intitule = (account?.intitule || '').toLowerCase();
    if (intitule.includes('épargne') || intitule.includes('epargne')) return 'Épargne';
    return 'Courant';
  }

  activateAccountByNum(numCompte: string) {
    const scrollY = window.scrollY;
    const account = this.accounts().find(a => a.numCompte === numCompte);
    if (!account) return;
    if (account.status === 'ACTIVATED') { alert('Ce compte est déjà activé.'); return; }
    if (!confirm('Activer ce compte ?\n\n• Activer le compte courant\n• Créer compte épargne (1.25%)\n• Créditer 200 € 🎁\n• Émettre carte')) return;
    this.loading.set(true);
    this.error.set(null);
    this.acc.activate(numCompte).pipe(
      switchMap(() => {
        this.accounts.update(list => list.map(a => a.numCompte === numCompte ? { ...a, status: 'ACTIVATED' } : a));
        this.clientsWithAccounts.update(clients => clients.map(client => ({
          ...client,
          comptes: client.comptes.map(compte => compte.numCompte === numCompte ? { ...compte, status: 'ACTIVATED' } : compte)
        })));
        return this.http.post<any>(`${this.API_BASE}/savings/${numCompte}/convertir`, { tauxInteret: 0.0125 }).pipe(
          catchError((e) => { console.warn('Épargne:', e?.error?.message); return of(null); })
        );
      }),
      switchMap(() =>
        this.http.post<any>(`${this.API_BASE}/operations/deposit`, {
          numCompte, montant: 200, description: '🎁 Bienvenue chez Techno-Bank'
        }).pipe(catchError((e) => { console.warn('Bienvenue:', e?.error?.message); return of(null); }))
      ),
      switchMap(() => {
        if (account.hasCard) return of(null);
        return this.cards.issue(numCompte).pipe(
          switchMap(() => this.cards.debloquer(numCompte).pipe(catchError(() => of(null)))),
          catchError(() => { this.error.set('⚠️ Compte activé mais carte KO'); return of(null); })
        );
      })
    ).subscribe({
      next: () => {
        this.loading.set(false);
        const clientId = this.selectedClientId();
        if (clientId) this.loadAccountSectionsForClient(clientId);
        this.loadAll(false);
        setTimeout(() => window.scrollTo({ top: scrollY, behavior: 'instant' }), 100);
        alert('✅ Activation complète');
      },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message ?? 'Activation KO'); }
    });
  }

  toggleSuspendCompteByNum(numCompte: string) {
    const scrollY = window.scrollY;
    const account = this.accounts().find(a => a.numCompte === numCompte);
    if (!account) return;
    const isSuspended = this.isAccountSuspended(account);
    if (!confirm(isSuspended ? 'Réactiver ce compte ?' : 'Suspendre ce compte ?')) return;
    this.loading.set(true);
    this.error.set(null);
    const request = isSuspended ? this.acc.activate(numCompte) : this.acc.suspend(numCompte);
    request.subscribe({
      next: () => {
        const newStatus = isSuspended ? 'ACTIVATED' : 'SUSPENDED';
        this.loading.set(false);
        this.accounts.update(list => list.map(a => a.numCompte === numCompte ? { ...a, status: newStatus } : a));
        this.accountSections.update(sections =>
          sections.map(s => s.account.numCompte === numCompte ? { ...s, account: { ...s.account, status: newStatus } } : s)
        );
        this.clientsWithAccounts.update(clients =>
          clients.map(client => ({
            ...client,
            comptes: client.comptes.map(compte => compte.numCompte === numCompte ? { ...compte, status: newStatus } : compte)
          }))
        );
        if (this.selected()?.numCompte === numCompte)
          this.selected.update(s => s ? { ...s, status: newStatus } : s);
        // ✅ Si compte suspendu, mettre à jour aussi selectedCourant dans Gestion Carte
        if (this.selectedCourant()?.numCompte === numCompte)
          this.selectedCourant.update(s => s ? { ...s, status: newStatus } : s);
        setTimeout(() => window.scrollTo({ top: scrollY, behavior: 'instant' }), 100);
        alert(isSuspended ? '✅ Compte réactivé' : '⏸️ Compte suspendu');
      },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message || e?.message || `Erreur HTTP ${e?.status}`); }
    });
  }

  activateAccount() { const s = this.selected(); if (s) this.activateAccountByNum(s.numCompte); }
  toggleSuspendCompte() { const s = this.selected(); if (s) this.toggleSuspendCompteByNum(s.numCompte); }
  issueCard() { const s = this.selected(); if (s) this.issueCardForAccount(s.numCompte); }

  issueCardForAccount(numCompte: string) {
    if (!confirm('Émettre une carte pour ce compte ?')) return;
    this.loading.set(true);
    this.error.set(null);
    this.cards.issue(numCompte).subscribe({
      next: () => { this.loading.set(false); alert('💳 Carte émise'); this.refreshSelected(); },
      error: (e: any) => { this.loading.set(false); this.error.set(e?.error?.message ?? 'Émission KO'); }
    });
  }

  toggleCard() {
    const s = this.selected(); const c = this.card();
    if (!s || !c) return;
    if (!confirm(c.estActive ? 'BLOQUER cette carte ?' : 'Débloquer cette carte ?')) return;
    this.loading.set(true);
    this.error.set(null);
    const req = c.estActive ? this.cards.bloquer(s.numCompte, 'admin') : this.cards.debloquer(s.numCompte);
    req.subscribe({
      next: () => { this.loading.set(false); alert(c.estActive ? '🔒 Carte bloquée' : '🔓 Carte débloquée'); this.refreshSelected(); },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message || e?.message || `Erreur HTTP ${e?.status}`); }
    });
  }

  toggleCardForAccount(numCompte: string, card: CardInfo | null) {
    if (!card) return;
    if (!confirm(card.estActive ? 'BLOQUER cette carte ?' : 'Débloquer cette carte ?')) return;
    this.loading.set(true);
    this.error.set(null);
    const req = card.estActive ? this.cards.bloquer(numCompte, 'admin') : this.cards.debloquer(numCompte);
    req.subscribe({
      next: () => {
        this.loading.set(false);
        alert(card.estActive ? '🔒 Carte bloquée' : '🔓 Carte débloquée');
        const clientId = this.selectedClientId();
        if (clientId) this.loadAccountSectionsForClient(clientId);
      },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message || e?.message || `Erreur HTTP ${e?.status}`); }
    });
  }

  setLimits() {
    const s = this.selected();
    if (!s) return;
    const j = Number(this.limitsForm.value.plafondJournalier);
    const m = Number(this.limitsForm.value.plafondMensuel);
    if (!j || !m) return;
    this.loading.set(true);
    this.error.set(null);
    this.cards.setPlafonds(s.numCompte, j, m).subscribe({
      next: () => { this.loading.set(false); alert('✅ Plafonds mis à jour'); this.refreshSelected(); },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message ?? 'Maj plafonds KO'); }
    });
  }

  getFilteredClients(): ClientWithAccounts[] {
    const filter = this.searchFilter().toLowerCase().trim();
    if (!filter) return this.clientsWithAccounts();
    return this.clientsWithAccounts().filter(client =>
      client.nom.toLowerCase().includes(filter) ||
      client.prenom.toLowerCase().includes(filter) ||
      client.email.toLowerCase().includes(filter) ||
      `${client.prenom} ${client.nom}`.toLowerCase().includes(filter) ||
      client.id.toString().includes(filter)
    );
  }

  getFilteredEmployees(): Employe[] {
    const filter = this.employeeSearch().toLowerCase().trim();
    if (!filter) return this.employees();
    return this.employees().filter(emp =>
      emp.nom?.toLowerCase().includes(filter) ||
      emp.prenom?.toLowerCase().includes(filter) ||
      emp.email?.toLowerCase().includes(filter) ||
      emp.role?.toLowerCase().includes(filter) ||
      emp.matricule?.toLowerCase().includes(filter) ||
      emp.poste?.toLowerCase().includes(filter) ||
      `${emp.prenom} ${emp.nom}`.toLowerCase().includes(filter) ||
      emp.id.toString().includes(filter)
    );
  }

  toggleClientAccounts(clientId: number) {
    const wasExpanded = this.expandedClientId() === clientId;
    this.expandedClientId.set(wasExpanded ? null : clientId);
    if (!wasExpanded) {
      const client = this.clientsWithAccounts().find(c => c.id === clientId);
      if (client && client.placements === undefined) this.loadPlacementsForExpandedClient(clientId);
    }
  }

  isClientExpanded(clientId: number): boolean { return this.expandedClientId() === clientId; }

  onSearchChange(value: string) { this.searchFilter.set(value); }
  onEmployeeSearchChange(value: string) { this.employeeSearch.set(value); }
  onAccountsSearchChange(value: string) { this.accountsSearch.set(value); }
  onCardsSearchChange(value: string) { this.cardsSearch.set(value); }

  refreshClients() {
    this.loadedTabs.delete('accounts');
    this.loadedTabs.delete('clients');
    this.loadedTabs.delete('cards');
    this.loadAll(true).then(() => {
      this.loadedTabs.add('accounts');
      this.loadedTabs.add('clients');
      this.loadedTabs.add('cards');
    });
  }

  refreshEmployees() { this.loadedTabs.delete('employees'); this.loadEmployees(true).then(() => this.loadedTabs.add('employees')); }
  refreshAgencyServices() { this.loadedTabs.delete('agencyServices'); this.loadAgencyServices(true).then(() => this.loadedTabs.add('agencyServices')); }
  refreshInvestments() { this.loadedTabs.delete('investments'); this.loadInvestments(true).then(() => this.loadedTabs.add('investments')); }
  refreshFunds() { this.loadedTabs.delete('funds'); this.loadFunds(true).then(() => this.loadedTabs.add('funds')); }

  selectEmployee(emp: Employe) { this.selectedEmployee.set(emp); }

  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  formatCurrency(amount: number | undefined | null): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(Number(amount || 0));
  }

  getAccountIcon(intitule: string): string {
    if (intitule?.toLowerCase().includes('épargne') || intitule?.toLowerCase().includes('epargne')) return '💰';
    return '🏦';
  }

  getGainJournalier(placement: any): number {
    const gainPrevu = Number(placement?.gainPrevu || 0);
    return gainPrevu > 0 ? gainPrevu / 365 : 0;
  }

  getNombreJoursDepuisPlacement(datePlacement: string | undefined): number {
    if (!datePlacement) return 0;
    const debut = new Date(datePlacement);
    const aujourdHui = new Date();
    debut.setHours(0,0,0,0); aujourdHui.setHours(0,0,0,0);
    const diffJours = Math.floor((aujourdHui.getTime() - debut.getTime()) / 86400000);
    return diffJours > 0 ? diffJours : 0;
  }

  getGainDuMoment(placement: any): number {
    const gainJournalier = this.getGainJournalier(placement);
    const nbJours = this.getNombreJoursDepuisPlacement(placement?.datePlacement);
    const gainPrevu = Number(placement?.gainPrevu || 0);
    const gainActuel = gainJournalier * nbJours;
    return gainActuel > gainPrevu ? gainPrevu : gainActuel;
  }

  getValeurDuMoment(placement: any): number {
    return Number(placement?.montant || 0) + this.getGainDuMoment(placement);
  }

  logout() {
    try { this.auth.logout(); } catch {}
    try { localStorage.clear(); sessionStorage.clear(); } catch {}
    this.router.navigate(['/auth'], { replaceUrl: true }).then(() => {
      setTimeout(() => { if (!window.location.pathname.includes('/auth')) window.location.href = '/auth'; }, 200);
    });
  }
}
