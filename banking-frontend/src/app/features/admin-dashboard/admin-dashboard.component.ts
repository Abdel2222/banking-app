import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { AuthService } from '../../core/services/auth.service';
import { AccountsService, BankAccount } from '../../core/services/accounts.service';
import { OperationsService, Operation } from '../../core/services/operations.service';
import { CardsService, CardInfo } from '../../core/services/cards.service';
import { VirementService, Client, Compte } from '../../core/services/virement.service';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { AdminChatbotComponent } from '../admin-chatbot/admin-chatbot.component';
import { InvestmentService, Fonds, Placement } from '../investments/investment.service';

interface CompteWithCard extends Compte {
  card?: CardInfo | null;
}

interface ClientWithAccounts extends Client {
  comptes: CompteWithCard[];
}

interface AdminAccount extends BankAccount {
  clientId?: number;
  clientName?: string;
  cardNumber?: string;
  cardActive?: boolean;
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

interface AdminRendezVousNotification {
  id: number;
  date: string;
  heure: string;
  typeOperation: string;
  agence: string;
  montant: number;
  statut: 'PLANIFIE' | 'TERMINE' | 'ANNULE';
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MoneyPipe, DatePipe, AdminChatbotComponent],
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

  private readonly API_BASE = 'http://localhost:8084/api';

  busy = signal<boolean>(false);
  me = signal<any>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  activeTab = signal<
    'accounts' | 'cards' | 'chatbot' | 'clients' | 'employees' | 'appointments' | 'investments' | 'funds'
  >('accounts');

  accounts = signal<AdminAccount[]>([]);
  selected = signal<AdminAccount | null>(null);
  card = signal<CardInfo | null>(null);
  ops = signal<Operation[]>([]);

  frais = signal<any[]>([]);
  showFraisModal = signal<boolean>(false);

  fraisForm = this.fb.group({
    montant: [2.89, [Validators.required, Validators.min(0.01)]],
    description: ['Frais de tenue de compte', Validators.required],
    typeFrais: ['TENUE_COMPTE', Validators.required],
    periodicite: ['MENSUEL', Validators.required]
  });

  clients = signal<Client[]>([]);
  clientsWithAccounts = signal<ClientWithAccounts[]>([]);
  loadingClients = signal<boolean>(false);
  errorClients = signal<string | null>(null);
  searchFilter = signal<string>('');
  totalClients = signal<number>(0);
  expandedClientId = signal<number | null>(null);

  employees = signal<Employe[]>([]);
  loadingEmployees = signal<boolean>(false);
  errorEmployees = signal<string | null>(null);
  employeeSearch = signal<string>('');
  selectedEmployee = signal<Employe | null>(null);

  appointments = signal<AdminRendezVousNotification[]>([]);
  loadingAppointments = signal<boolean>(false);
  errorAppointments = signal<string | null>(null);

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

  constructor() {
    const token = localStorage.getItem('auth_token');
    if (!token || !this.auth.isAdmin()) {
      window.location.replace('/auth');
      return;
    }
    const fromToken = this.auth.currentUser();
    this.me.set(fromToken);
  }

  ngOnInit() {
    setTimeout(() => {
      const token = localStorage.getItem('auth_token');
      if (!token || !this.auth.isAdmin()) {
        window.location.replace('/auth');
        return;
      }
      this.loadAll();
      this.loadEmployees();
      this.loadInvestments();
      this.loadFunds();
      this.loadDemoAppointments();
    }, 300);
  }

  switchTab(tab: 'accounts' | 'cards' | 'chatbot' | 'clients' | 'employees' | 'appointments' | 'investments' | 'funds') {
    this.activeTab.set(tab);

    if ((tab === 'clients' || tab === 'accounts' || tab === 'cards') && this.clientsWithAccounts().length === 0) {
      this.loadAll();
    }

    if (tab === 'employees' && this.employees().length === 0) {
      this.loadEmployees();
    }

    if (tab === 'appointments' && this.appointments().length === 0) {
      this.loadDemoAppointments();
    }

    if (tab === 'investments' && this.investments().length === 0) {
      this.loadInvestments();
    }

    if (tab === 'funds' && this.fonds().length === 0) {
      this.loadFunds();
    }
  }

  loadAll() {
    const token = localStorage.getItem('auth_token');
    if (!token || !this.auth.isAdmin()) {
      this.error.set('Session expirée. Veuillez vous reconnecter.');
      this.errorClients.set('Session expirée. Veuillez vous reconnecter.');
      this.loading.set(false);
      this.loadingClients.set(false);
      setTimeout(() => window.location.replace('/auth'), 500);
      return;
    }

    this.loading.set(true);
    this.loadingClients.set(true);
    this.error.set(null);
    this.errorClients.set(null);

    this.virementService.getAllClients().subscribe({
      next: (response: any) => {
        const clients: Client[] = Array.isArray(response) ? response : (response?.data ?? []);
        this.clients.set(clients);
        this.totalClients.set(clients.length);

        if (clients.length === 0) {
          this.accounts.set([]);
          this.clientsWithAccounts.set([]);
          this.selected.set(null);
          this.card.set(null);
          this.ops.set([]);
          this.loading.set(false);
          this.loadingClients.set(false);
          return;
        }

        this.loadClientsAccountsAndCards(clients);
      },
      error: () => {
        this.error.set('Impossible de charger les clients');
        this.errorClients.set('Impossible de charger les clients');
        this.loading.set(false);
        this.loadingClients.set(false);
      }
    });
  }

  private async loadClientsAccountsAndCards(clients: Client[]) {
    const previousSelectedNum = this.selected()?.numCompte;

    try {
      const clientsResults: ClientWithAccounts[] = [];
      const flatAccounts: AdminAccount[] = [];

      for (const client of clients) {
        let comptes: Compte[] = [];
        try {
          comptes = await firstValueFrom(this.virementService.getComptesClient(client.id));
        } catch {
          comptes = [];
        }

        const comptesWithCards: CompteWithCard[] = comptes.map(compte => ({ ...compte, card: null }));

        for (const compte of comptes) {
          flatAccounts.push({
            id: compte.id,
            numCompte: compte.numCompte,
            balance: compte.balance,
            solde: compte.balance,
            status: compte.status,
            intitule: compte.intitule,
            devise: compte.devise,
            clientId: client.id,
            clientName: `${client.prenom} ${client.nom}`,
            hasCard: compte.hasCard ?? false,
            cardNumber: undefined,
            cardActive: undefined,
            savingsAccount: compte.savingsAccount || compte.intitule?.toLowerCase().includes('épargne')
          });
        }

        clientsResults.push({ ...client, comptes: comptesWithCards });
      }

      this.clientsWithAccounts.set(clientsResults);
      this.accounts.set(flatAccounts);

      if (flatAccounts.length > 0) {
        const selectedAgain = previousSelectedNum ? flatAccounts.find(a => a.numCompte === previousSelectedNum) : null;
        this.select(selectedAgain || flatAccounts[0]);
      } else {
        this.selected.set(null);
        this.card.set(null);
        this.ops.set([]);
      }
    } finally {
      this.loading.set(false);
      this.loadingClients.set(false);
    }
  }

  select(a: AdminAccount) {
    this.selected.set(a);
    this.error.set(null);
    this.loadCard(a.numCompte);
    this.loadOps(a.numCompte);
    this.loadFraisForSelected();
  }

  private loadCard(num: string) {
    this.cards.getByAccount(num).subscribe({
      next: (c) => {
        this.card.set(c ?? null);
        if (this.selected()?.numCompte === num && c) {
          this.selected.update(s => s ? {
            ...s, hasCard: true, cardNumber: c.numeroCarte, cardActive: c.estActive
          } : s);
        }
      },
      error: () => this.card.set(null)
    });
  }

  private normalizeOp(o: any): Operation {
    return {
      id: o?.id ?? 0,
      montant: Number(o?.montant ?? 0),
      date: o?.date ?? o?.dateOperation ?? null,
      description: o?.description ?? o?.libelle ?? o?.type ?? ''
    };
  }

  private loadOps(num: string) {
    this.opsApi.recent(num, 5).subscribe({
      next: (rows) => this.ops.set((rows ?? []).map(r => this.normalizeOp(r))),
      error: () => this.ops.set([])
    });
  }

  private loadFraisForSelected() {
    const clientId = this.selected()?.clientId;
    if (!clientId) {
      this.frais.set([]);
      return;
    }

    this.http.get<any>(`${this.API_BASE}/frais/client/${clientId}`).subscribe({
      next: (resp) => {
        const list = resp?.data ?? (Array.isArray(resp) ? resp : []);
        this.frais.set(list);
      },
      error: () => this.frais.set([])
    });
  }

  openFraisModal() { this.showFraisModal.set(true); }
  closeFraisModal() { this.showFraisModal.set(false); }

  submitFrais() {
    const clientId = this.selected()?.clientId;
    if (!clientId || this.fraisForm.invalid) {
      this.error.set('Sélectionne un compte client');
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

    this.loading.set(true);
    this.http.post(`${this.API_BASE}/frais/client/${clientId}`, body).subscribe({
      next: () => {
        this.loading.set(false);
        this.closeFraisModal();
        this.loadFraisForSelected();
        alert('✅ Frais ajouté avec succès');
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set('Erreur ajout frais: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  supprimerFrais(fraisId: number) {
    if (!confirm('Supprimer ce frais de gestion ?')) return;

    this.loading.set(true);
    this.http.delete(`${this.API_BASE}/frais/${fraisId}`).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadFraisForSelected();
        alert('✅ Frais supprimé');
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set('Erreur suppression: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  facturerSurCeCompte() {
    const numCompte = this.selected()?.numCompte;
    if (!numCompte) return;

    const montantStr = prompt('Montant à débiter (€) ?', '2.89');
    if (!montantStr) return;
    const montant = parseFloat(montantStr.replace(',', '.'));
    if (isNaN(montant) || montant <= 0) {
      alert('Montant invalide');
      return;
    }

    if (!confirm(`Débiter ${montant} € de frais sur ce compte ?`)) return;

    this.loading.set(true);
    this.http.post<any>(
      `${this.API_BASE}/frais/fees/gestion/${numCompte}?montant=${montant}&autoriserDecouvert=false`,
      {}
    ).subscribe({
      next: (resp) => {
        this.loading.set(false);
        if (resp?.success) {
          alert(`✅ Frais débité !\nAvant: ${resp.before} €\nAprès: ${resp.after} €`);
        } else {
          alert('⚠️ ' + (resp?.message || 'Échec'));
        }
        this.refreshSelected();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set('Erreur facturation: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  facturerTousLesComptes() {
    const montantStr = prompt('Montant à débiter sur TOUS les comptes (€) ?', '2.89');
    if (!montantStr) return;
    const montant = parseFloat(montantStr.replace(',', '.'));
    if (isNaN(montant) || montant <= 0) {
      alert('Montant invalide');
      return;
    }

    if (!confirm(`Débiter ${montant} € de frais sur TOUS les comptes actifs ?`)) return;

    this.loading.set(true);
    this.http.post<any>(
      `${this.API_BASE}/frais/fees/gestion/apply-all?montant=${montant}&autoriserDecouvert=false`,
      {}
    ).subscribe({
      next: (resp) => {
        this.loading.set(false);
        const stats = resp?.stats;
        alert(
          `✅ Facturation effectuée !\n\n` +
          `Total comptes : ${stats?.total || 0}\n` +
          `Frais appliqués : ${stats?.appliques || 0}\n` +
          `Solde insuffisant : ${stats?.soldeInsuffisant || 0}\n` +
          `Erreurs : ${stats?.erreurs || 0}`
        );
        this.refreshSelected();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set('Erreur facturation: ' + (e?.error?.message || e?.message || ''));
      }
    });
  }

  loadEmployees() {
    this.loadingEmployees.set(true);
    this.errorEmployees.set(null);

    this.http.get<Employe[]>(`${this.API_BASE}/employes`).subscribe({
      next: (resp) => {
        this.employees.set(Array.isArray(resp) ? resp : []);
        this.loadingEmployees.set(false);
      },
      error: () => {
        this.errorEmployees.set('Impossible de charger les employés');
        this.loadingEmployees.set(false);
      }
    });
  }

  loadDemoAppointments() {
    this.loadingAppointments.set(true);
    this.errorAppointments.set(null);

    const demoData: AdminRendezVousNotification[] = [
      {
        id: 1,
        date: '18/05/2026',
        heure: '09:30',
        typeOperation: 'DÉPÔT',
        agence: 'Bruxelles Centre - Grand Place',
        montant: 2500,
        statut: 'PLANIFIE'
      },
      {
        id: 2,
        date: '19/05/2026',
        heure: '14:00',
        typeOperation: 'RETRAIT',
        agence: 'Ixelles - Flagey',
        montant: 1200,
        statut: 'PLANIFIE'
      },
      {
        id: 3,
        date: '20/05/2026',
        heure: '11:15',
        typeOperation: 'DÉPÔT',
        agence: 'Bruxelles Nord - Gare du Nord',
        montant: 5000,
        statut: 'TERMINE'
      }
    ];

    setTimeout(() => {
      this.appointments.set(demoData);
      this.loadingAppointments.set(false);
    }, 300);
  }

  loadInvestments() {
    this.loadingInvestments.set(true);
    this.errorInvestments.set(null);

    this.investmentService.getTousLesPlacements().subscribe({
      next: (resp) => {
        this.investments.set(resp || []);
        this.loadingInvestments.set(false);
      },
      error: () => {
        this.errorInvestments.set('Impossible de charger les investissements');
        this.loadingInvestments.set(false);
      }
    });
  }

  loadFunds() {
    this.loadingFunds.set(true);
    this.errorFunds.set(null);

    this.investmentService.getTousLesFonds().subscribe({
      next: (resp) => {
        this.fonds.set(resp || []);
        this.loadingFunds.set(false);
      },
      error: () => {
        this.errorFunds.set('Impossible de charger les fonds');
        this.loadingFunds.set(false);
      }
    });
  }

  showClientFunds(clientId: number, clientName: string) {
    this.loadingInvestments.set(true);
    this.errorInvestments.set(null);
    this.selectedClientName.set(clientName);

    this.investmentService.getPlacementsByClient(clientId).subscribe({
      next: (resp) => {
        this.selectedClientFunds.set(resp || []);
        this.loadingInvestments.set(false);
        this.activeTab.set('funds');
      },
      error: () => {
        this.selectedClientFunds.set([]);
        this.errorInvestments.set('Impossible de charger les placements du client');
        this.loadingInvestments.set(false);
      }
    });
  }

  clearSelectedClientFunds() {
    this.selectedClientFunds.set([]);
    this.selectedClientName.set('');
  }

  refreshSelected() {
    this.loadAll();
    this.loadInvestments();
    this.loadFunds();
  }

  isSelectedSuspended(): boolean {
    return this.isAccountSuspended(this.selected());
  }

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

  activateAccount() {
    const s = this.selected();
    if (!s) return;
    if (s.status === 'ACTIVATED') { alert('Ce compte est déjà activé.'); return; }
    if (!confirm('Activer ce compte ?')) return;

    this.loading.set(true);
    this.error.set(null);
    this.acc.activate(s.numCompte).subscribe({
      next: () => { this.loading.set(false); alert('✅ Compte activé'); this.refreshSelected(); },
      error: (e) => { this.loading.set(false); this.error.set(e?.error?.message ?? 'Activation KO'); }
    });
  }

  toggleSuspendCompte() {
    const s = this.selected();
    if (!s) return;
    const isSuspended = this.isAccountSuspended(s);
    if (!confirm(isSuspended ? 'Réactiver ce compte ?' : 'Suspendre ce compte ?')) return;

    this.loading.set(true);
    this.error.set(null);
    const request = isSuspended ? this.acc.activate(s.numCompte) : this.acc.suspend(s.numCompte);
    request.subscribe({
      next: () => {
        this.loading.set(false);
        alert(isSuspended ? '✅ Compte réactivé' : '⏸️ Compte suspendu');
        this.refreshSelected();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message || e?.error?.error || e?.message || `Erreur HTTP ${e?.status}`);
      }
    });
  }

  issueCard() {
    const s = this.selected();
    if (!s) return;
    if (!confirm('Émettre une carte pour ce compte ?')) return;

    this.loading.set(true);
    this.error.set(null);
    this.cards.issue(s.numCompte).subscribe({
      next: () => { this.loading.set(false); alert('💳 Carte émise avec succès'); this.refreshSelected(); },
      error: (e: any) => { this.loading.set(false); this.error.set(e?.error?.message ?? 'Émission carte KO'); }
    });
  }

  toggleCard() {
    const s = this.selected();
    const c = this.card();
    if (!s || !c) return;
    if (!confirm(c.estActive ? 'BLOQUER cette carte ?' : 'Débloquer cette carte ?')) return;

    this.loading.set(true);
    this.error.set(null);
    const req = c.estActive ? this.cards.bloquer(s.numCompte, 'admin') : this.cards.debloquer(s.numCompte);
    req.subscribe({
      next: () => { this.loading.set(false); alert(c.estActive ? '🔒 Carte bloquée' : '🔓 Carte débloquée'); this.refreshSelected(); },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message || e?.error?.error || e?.message || `Erreur HTTP ${e?.status}`);
      }
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
    this.expandedClientId.set(this.expandedClientId() === clientId ? null : clientId);
  }

  isClientExpanded(clientId: number): boolean {
    return this.expandedClientId() === clientId;
  }

  onSearchChange(value: string) { this.searchFilter.set(value); }
  onEmployeeSearchChange(value: string) { this.employeeSearch.set(value); }

  refreshClients() { this.loadAll(); }
  refreshEmployees() { this.loadEmployees(); }
  refreshAppointments() { this.loadDemoAppointments(); }
  refreshInvestments() { this.loadInvestments(); }
  refreshFunds() { this.loadFunds(); }

  selectEmployee(emp: Employe) {
    this.selectedEmployee.set(emp);
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(amount || 0);
  }

  getAccountIcon(intitule: string): string {
    if (intitule?.toLowerCase().includes('épargne')) return '💰';
    return '🏦';
  }

  logout() {
    this.auth.logout();
    localStorage.removeItem('auth_token');
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('user');
    localStorage.removeItem('role');
    sessionStorage.clear();
    window.location.replace('/auth');
  }
}
