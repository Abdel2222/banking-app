import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { VirementService, Client, Compte, VirementRequest } from '../../core/services/virement.service';
import { OperationsStoreService } from '../../core/services/operations-store.service';

@Component({
  selector: 'app-virement',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './virement.component.html',
  styleUrls: ['./virement.component.scss']
})
export class VirementComponent implements OnInit {
  // Signals pour la gestion d'état
  clients = signal<Client[]>([]);
  mesComptes = signal<Compte[]>([]);
  comptesDestinataire = signal<Compte[]>([]);

  selectedClientDestinataire = signal<Client | null>(null);
  selectedCompteSource = signal<Compte | null>(null);
  selectedCompteDestinataire = signal<Compte | null>(null);

  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  currentStep = signal<number>(1);
  searchFilter = '';

  // NOUVEAUX Signals
  communicationStructuree = signal<boolean>(false);
  virementInterne = signal<boolean>(false);

  virementForm: FormGroup;

  // Injections
  private http = inject(HttpClient);
  private operationsStore = inject(OperationsStoreService);

  constructor(
    private virementService: VirementService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.virementForm = this.fb.group({
      montant: ['', [Validators.required, Validators.min(0.01), Validators.max(10000)]],
      communication: ['Virement', [Validators.required, Validators.minLength(3)]],
      description: [''],
      communicationOGM: ['']
    });
  }

  ngOnInit() {
    console.log('🚀 [COMPONENT] Initialisation VirementComponent');
    this.loadClients();
    this.loadMesComptes();
  }

  loadClients() {
    console.log('📡 [COMPONENT] loadClients - Début');
    this.loading.set(true);
    this.error.set(null);

    this.virementService.getAllClients().subscribe({
      next: (response) => {
        console.log('📥 [COMPONENT] Réponse getAllClients:', response);
        if (response.success) {
          this.clients.set(response.data);
          console.log('✅ [COMPONENT] Clients chargés:', response.data.length);
        } else {
          this.error.set('Erreur lors du chargement des clients');
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('❌ [COMPONENT] Erreur loadClients:', err);
        this.error.set('Impossible de charger les clients');
        this.loading.set(false);
      }
    });
  }

  loadMesComptes() {
    console.log('📡 [COMPONENT] loadMesComptes - Début');

    this.virementService.getCurrentUserComptes().subscribe({
      next: (comptes) => {
        console.log('✅ [COMPONENT] Mes comptes chargés:', comptes);
        this.mesComptes.set(comptes);

        if (comptes.length > 0) {
          this.selectedCompteSource.set(comptes[0]);
          console.log('✅ [COMPONENT] Premier compte sélectionné:', comptes[0].numCompte);
        } else {
          console.warn('⚠️ [COMPONENT] Aucun compte trouvé');
        }
      },
      error: (error) => {
        console.error('❌ [COMPONENT] Erreur loadMesComptes:', error);
      }
    });
  }

  onCompteSourceSelect(numCompte: string) {
    console.log('🎯 [COMPONENT] onCompteSourceSelect:', numCompte);

    const compte = this.mesComptes().find(c => c.numCompte === numCompte);
    if (compte) {
      this.selectedCompteSource.set(compte);
      console.log('✅ [COMPONENT] Compte source sélectionné:', compte.numCompte, compte.balance, '€');
    } else {
      console.warn('⚠️ [COMPONENT] Compte non trouvé:', numCompte);
    }
  }

  toggleVirementInterne() {
    this.virementInterne.update(v => !v);

    if (this.virementInterne()) {
      this.comptesDestinataire.set(this.mesComptes());
      this.selectedClientDestinataire.set(null);
      this.selectedCompteDestinataire.set(null);
      this.currentStep.set(2);
      console.log('✅ [COMPONENT] Mode virement interne activé');
    } else {
      this.comptesDestinataire.set([]);
      this.selectedCompteDestinataire.set(null);
      this.currentStep.set(1);
      console.log('✅ [COMPONENT] Mode virement externe activé');
    }
  }

  toggleCommunicationStructuree() {
    this.communicationStructuree.update(v => !v);

    if (this.communicationStructuree()) {
      this.virementForm.get('communicationOGM')?.setValidators([
        Validators.required,
        Validators.pattern(/^\+\+\+\d{3}\/\d{4}\/\d{5}\+\+\+$/)
      ]);
      this.virementForm.get('communication')?.clearValidators();
      this.virementForm.get('communication')?.setValue('');
    } else {
      this.virementForm.get('communication')?.setValidators([
        Validators.required,
        Validators.minLength(3)
      ]);
      this.virementForm.get('communicationOGM')?.clearValidators();
      this.virementForm.get('communicationOGM')?.setValue('');
    }

    this.virementForm.get('communication')?.updateValueAndValidity();
    this.virementForm.get('communicationOGM')?.updateValueAndValidity();
  }

  genererCommunicationOGM() {
    const part1 = String(Math.floor(Math.random() * 1000)).padStart(3, '0');
    const part2 = String(Math.floor(Math.random() * 10000)).padStart(4, '0');
    const baseNumber = parseInt(part1 + part2);
    const modulo = baseNumber % 97;
    const part3 = String(modulo === 0 ? 97 : modulo).padStart(5, '0');
    const ogm = `+++${part1}/${part2}/${part3}+++`;
    this.virementForm.get('communicationOGM')?.setValue(ogm);
    console.log('✅ [COMPONENT] Communication OGM générée:', ogm);
  }

  onClientDestinataireSelect(clientId: string) {
    if (!clientId || clientId === '') {
      this.selectedClientDestinataire.set(null);
      this.comptesDestinataire.set([]);
      this.selectedCompteDestinataire.set(null);
      this.currentStep.set(1);
      return;
    }

    const client = this.clients().find(c => c.id.toString() === clientId);

    if (client) {
      this.selectedClientDestinataire.set(client);
      this.loadComptesDestinataire(client.id);
    } else {
      this.error.set('Client introuvable');
    }
  }

  loadComptesDestinataire(clientId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.comptesDestinataire.set([]);

    this.virementService.getComptesClient(clientId).subscribe({
      next: (comptes: Compte[]) => {
        this.comptesDestinataire.set(comptes);

        if (comptes.length > 0) {
          this.selectedCompteDestinataire.set(comptes[0]);
          this.currentStep.set(2);
        } else {
          this.error.set('Aucun compte disponible pour ce client');
        }

        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        let errorMessage = 'Impossible de charger les comptes du destinataire';

        if (error.status === 401 || error.status === 403) {
          errorMessage = 'Session expirée. Veuillez vous reconnecter.';
        } else if (error.status === 404) {
          errorMessage = 'Client introuvable ou aucun compte associé';
        }

        this.error.set(errorMessage);
        this.loading.set(false);
      }
    });
  }

  onCompteDestinataireSelect(numCompte: string) {
    const compte = this.virementInterne()
      ? this.mesComptes().find(c => c.numCompte === numCompte)
      : this.comptesDestinataire().find(c => c.numCompte === numCompte);

    if (compte) {
      this.selectedCompteDestinataire.set(compte);
      this.currentStep.set(3);
      console.log('✅ [COMPONENT] Compte destinataire sélectionné:', compte.numCompte);
      console.log('   Type de compte:', compte.savingsAccount ? 'ÉPARGNE' : 'COURANT');
    }
  }

  confirmerVirement() {
    console.log('💸 [COMPONENT] === CONFIRMATION VIREMENT ===');

    if (this.virementForm.invalid || !this.selectedCompteSource() || !this.selectedCompteDestinataire()) {
      this.error.set('Veuillez remplir tous les champs');
      return;
    }

    const formValue = this.virementForm.value;
    const montant = parseFloat(formValue.montant);

    if (this.selectedCompteSource()!.balance < montant) {
      this.error.set(`Solde insuffisant. Disponible: ${this.formatCurrency(this.selectedCompteSource()!.balance)}`);
      return;
    }

    const communication = this.communicationStructuree()
      ? formValue.communicationOGM
      : formValue.communication;

    const isDestinationEpargne = this.selectedCompteDestinataire()!.savingsAccount === true;

    if (this.virementInterne() && isDestinationEpargne) {
      console.log('💎 [COMPONENT] Virement vers compte épargne détecté');
      this.alimenterEpargne(montant, communication);
      return;
    }

    const virementData: VirementRequest = {
      numCompteSource: this.selectedCompteSource()!.numCompte,
      numCompteDestinataire: this.selectedCompteDestinataire()!.numCompte,
      montant: montant,
      communication: String(communication || '').trim(),
      description: formValue.description
    };

    console.log('📤 [COMPONENT] Virement normal:', virementData);

    this.loading.set(true);
    this.error.set(null);

    this.virementService.effectuerVirement(virementData).subscribe({
      next: () => {
        const destinataire = this.virementInterne()
          ? `votre ${this.selectedCompteDestinataire()?.intitule || 'compte'}`
          : `${this.selectedClientDestinataire()?.prenom} ${this.selectedClientDestinataire()?.nom}`;

        // ========== ENREGISTRER L'OPÉRATION ==========
        this.operationsStore.addVirement(
          this.selectedCompteSource()!.numCompte,
          this.selectedCompteDestinataire()!.numCompte,
          montant,
          String(communication || ''),
          this.selectedCompteSource()!.intitule,
          this.selectedCompteDestinataire()!.intitule
        );
        // ============================================

        this.success.set(`Virement de ${this.formatCurrency(montant)} effectué avec succès vers ${destinataire}`);
        setTimeout(() => this.router.navigate(['/dashboard']), 3000);
        this.loading.set(false);
      },
      error: (error: Error) => {
        console.error('❌ [COMPONENT] Erreur virement:', error);
        this.error.set(error.message || 'Erreur lors du virement');
        this.loading.set(false);
      }
    });
  }

  alimenterEpargne(montant: number, communication: string) {
    console.log('💎 [COMPONENT] === ALIMENTATION COMPTE ÉPARGNE ===');
    console.log('   Montant:', montant, '€');
    console.log('   Compte bancaire source:', this.selectedCompteSource()!.numCompte);
    console.log('   Communication:', communication);

    this.loading.set(true);
    this.error.set(null);

    const token = localStorage.getItem('auth_token');
    const numCompteBancaire = this.selectedCompteSource()!.numCompte;

    this.http.post<any>(
      `/api/savings/${numCompteBancaire}/alimenter`,
      { montant },
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    ).subscribe({
      next: (response) => {
        console.log('✅ [COMPONENT] Épargne alimentée avec succès:', response);

        // ========== ENREGISTRER L'OPÉRATION ==========
        this.operationsStore.addVirement(
          this.selectedCompteSource()!.numCompte,
          this.selectedCompteDestinataire()!.numCompte,
          montant,
          communication || 'Alimentation épargne',
          this.selectedCompteSource()!.intitule,
          this.selectedCompteDestinataire()!.intitule
        );
        // ============================================

        this.success.set(`Virement de ${this.formatCurrency(montant)} effectué avec succès vers votre Compte Épargne`);
        setTimeout(() => this.router.navigate(['/dashboard']), 3000);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('❌ [COMPONENT] Erreur alimentation épargne:', error);
        this.error.set(error.error?.message || 'Erreur lors de l\'alimentation du compte épargne');
        this.loading.set(false);
      }
    });
  }

  retourDashboard() {
    this.router.navigate(['/dashboard']);
  }

  resetForm() {
    this.virementForm.reset({ communication: 'Virement' });
    this.selectedClientDestinataire.set(null);
    this.selectedCompteDestinataire.set(null);
    this.comptesDestinataire.set([]);
    this.currentStep.set(1);
    this.error.set(null);
    this.success.set(null);
    this.searchFilter = '';
    this.communicationStructuree.set(false);
    this.virementInterne.set(false);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  getFilteredClients(): Client[] {
    if (!this.searchFilter.trim()) {
      return this.clients();
    }

    const filter = this.searchFilter.toLowerCase().trim();
    return this.clients().filter(c =>
      c.nom.toLowerCase().includes(filter) ||
      c.prenom.toLowerCase().includes(filter) ||
      c.email.toLowerCase().includes(filter)
    );
  }

  get montantError() {
    const c = this.virementForm.get('montant');
    if (c?.errors && c.touched) {
      if (c.errors['required']) return 'Le montant est requis';
      if (c.errors['min']) return 'Le montant doit être > 0';
      if (c.errors['max']) return 'Maximum 10 000€';
    }
    return null;
  }

  get communicationError() {
    const c = this.virementForm.get('communication');
    if (c?.errors && c.touched) {
      if (c.errors['required']) return 'La communication est requise';
      if (c.errors['minlength']) return 'Minimum 3 caractères';
    }
    return null;
  }

  get communicationOGMError() {
    const c = this.virementForm.get('communicationOGM');
    if (c?.errors && c.touched) {
      if (c.errors['required']) return 'Communication OGM requise';
      if (c.errors['pattern']) return 'Format invalide (ex: +++123/4567/89012+++)';
    }
    return null;
  }
}
