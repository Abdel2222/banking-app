import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
 import { firstValueFrom } from 'rxjs'; // ✅ Import en haut du fichier

import { AuthService } from '../../core/services/auth.service';
import { AccountsService, BankAccount } from '../../core/services/accounts.service';
import { OperationsService, Operation } from '../../core/services/operations.service';
import { CardsService, CardInfo } from '../../core/services/cards.service';
import { VirementService, Client, Compte } from '../../core/services/virement.service';
import { HttpClient } from '@angular/common/http';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { AdminChatbotComponent } from '../admin-chatbot/admin-chatbot.component';

interface ClientWithAccounts extends Client {
  comptes: Compte[];
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MoneyPipe, DatePipe, AdminChatbotComponent],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private acc = inject(AccountsService);
  private opsApi = inject(OperationsService);
  private cards = inject(CardsService);
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);
  private virementService = inject(VirementService);

  busy = signal<boolean>(false);
  me = signal<any>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  // Navigation onglets
  activeTab = signal<'accounts' | 'chatbot' | 'clients'>('accounts');

  // Gestion des comptes
  accounts = signal<BankAccount[]>([]);
  selected = signal<BankAccount | null>(null);
  card = signal<CardInfo | null>(null);
  ops = signal<Operation[]>([]);

  // Gestion des clients
  clients = signal<Client[]>([]);
  clientsWithAccounts = signal<ClientWithAccounts[]>([]);
  loadingClients = signal<boolean>(false);
  errorClients = signal<string | null>(null);
  searchFilter = signal<string>('');
  totalClients = signal<number>(0);
  expandedClientId = signal<number | null>(null);

  limitsForm = this.fb.group({
    plafondJournalier: [250, [Validators.required, Validators.min(1)]],
    plafondMensuel: [1000, [Validators.required, Validators.min(1)]],
  });

  constructor() {
    if (!this.auth.isAdmin()) {
      this.router.navigateByUrl('/dashboard');
      return;
    }

    const fromToken = this.auth.currentUser();
    this.me.set(fromToken);
    this.loadAll();
  }

  // Méthode pour changer d'onglet
  switchTab(tab: 'accounts' | 'chatbot' | 'clients') {
    this.activeTab.set(tab);
    if (tab === 'clients' && this.clientsWithAccounts().length === 0) {
      this.loadAllClients();
    }
  }

  // ----------- LOADERS COMPTES -----------
  loadAll() {
    this.loading.set(true);
    this.acc.list().subscribe({
      next: (list) => {
        this.accounts.set(list ?? []);
        if (list?.length) this.select(list[0]);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e?.error?.message ?? 'Erreur chargement comptes');
        this.loading.set(false);
      }
    });
  }

  select(a: BankAccount) {
    this.selected.set(a);
    this.loadCard(a.numCompte);
    this.loadOps(a.numCompte);
  }

  private loadCard(num: string) {
    this.cards.getByAccount(num).subscribe({
      next: (c) => this.card.set(c ?? null),
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

  refreshSelected() {
    const s = this.selected();
    if (!s) return;
    this.loadCard(s.numCompte);
    this.loadOps(s.numCompte);
    this.acc.list().subscribe({
      next: (list) => {
        this.accounts.set(list ?? []);
        const found = (list ?? []).find(x => x.numCompte === s.numCompte) || s;
        this.selected.set(found);
      }
    });
  }

  // ----------- ACTIONS ADMIN COMPTES -----------
  activateAccount() {
    const s = this.selected();
    if (!s) return;
    this.loading.set(true);
    this.error.set(null);
    this.acc.activate(s.numCompte).subscribe({
      next: () => {
        this.loading.set(false);
        this.refreshSelected();
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Activation KO');
      }
    });
  }

  issueCard() {
    const s = this.selected();
    if (!s) return;
    this.loading.set(true);
    this.error.set(null);
    this.http.post(`/api/cartes/${s.numCompte}/issue`, {}).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadCard(s.numCompte);
      },
      error: (e: any) => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Émission carte KO');
      }
    });
  }

  toggleCard() {
    const s = this.selected();
    const c = this.card();
    if (!s || !c) return;
    this.loading.set(true);
    this.error.set(null);
    const req = c.estActive
      ? this.cards.bloquer(s.numCompte, 'admin')
      : this.cards.debloquer(s.numCompte);
    req.subscribe({
      next: () => {
        this.loading.set(false);
        this.loadCard(s.numCompte);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Erreur carte');
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
      next: () => {
        this.loading.set(false);
        this.loadCard(s.numCompte);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? 'Maj plafonds KO');
      }
    });
  }



// ----------- GESTION DES CLIENTS -----------
loadAllClients() {
  this.loadingClients.set(true);
  this.errorClients.set(null);

  this.virementService.getAllClients().subscribe({
    next: (response) => {
      if (response.success) {
        this.clients.set(response.data);
        this.totalClients.set(response.data.length);
        console.log('✅ Clients chargés:', response.data);
        // Charger les comptes pour chaque client
        this.loadClientsAccounts(response.data);
      } else {
        this.errorClients.set('Erreur lors du chargement des clients');
        this.loadingClients.set(false);
      }
    },
    error: (error) => {
      console.error('❌ Error loading clients:', error);
      this.errorClients.set('Impossible de charger la liste des clients');
      this.loadingClients.set(false);
    }
  });
}

// ✅ VERSION ANGULAR 19 : Utiliser firstValueFrom au lieu de toPromise()
async loadClientsAccounts(clients: Client[]) {
  const clientsPromises = clients.map(async (client) => {
    try {
      // ✅ CORRECTION pour Angular 19
      const comptes = await firstValueFrom(
        this.virementService.getComptesClient(client.id)
      );
      console.log(`✅ Comptes pour client ${client.id} (${client.prenom} ${client.nom}):`, comptes);
      return {
        ...client,
        comptes: comptes || []
      };
    } catch (error) {
      console.error(`❌ Erreur chargement comptes client ${client.id}:`, error);
      return {
        ...client,
        comptes: []
      };
    }
  });

  try {
    const results = await Promise.all(clientsPromises);
    console.log('✅ Tous les clients avec comptes:', results);
    this.clientsWithAccounts.set(results);
  } catch (error) {
    console.error('❌ Erreur lors du chargement des comptes:', error);
  } finally {
    this.loadingClients.set(false);
  }
}
  getFilteredClients(): ClientWithAccounts[] {
    const filter = this.searchFilter().toLowerCase().trim();

    if (!filter) {
      return this.clientsWithAccounts();
    }

    return this.clientsWithAccounts().filter(client =>
      client.nom.toLowerCase().includes(filter) ||
      client.prenom.toLowerCase().includes(filter) ||
      client.email.toLowerCase().includes(filter) ||
      `${client.prenom} ${client.nom}`.toLowerCase().includes(filter) ||
      client.id.toString().includes(filter)
    );
  }

  // Basculer l'affichage des comptes
  toggleClientAccounts(clientId: number) {
    if (this.expandedClientId() === clientId) {
      this.expandedClientId.set(null);
    } else {
      this.expandedClientId.set(clientId);
    }
  }

  isClientExpanded(clientId: number): boolean {
    return this.expandedClientId() === clientId;
  }

  onSearchChange(value: string) {
    this.searchFilter.set(value);
  }

  refreshClients() {
    this.clientsWithAccounts.set([]);
    this.loadAllClients();
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  // Méthode utilitaire pour identifier le type de compte
  getAccountIcon(intitule: string): string {
    if (intitule.toLowerCase().includes('épargne')) {
      return '💰';
    }
    return '🏦';
  }

  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/auth');
  }
}
