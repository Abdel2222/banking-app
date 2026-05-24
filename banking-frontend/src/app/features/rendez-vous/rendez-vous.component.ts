import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { AuthService } from '../../core/services/auth.service';
import { OperationsService } from '../../core/services/operations.service';
import { OperationsStoreService } from '../../core/services/operations-store.service';
import { NotificationService } from '../../core/services/notification.service';

interface Employe {
  id: number;
  prenom: string;
  nom: string;
  email: string;
  role: string;
  matricule?: string;
  poste?: string;
}

interface Guichet {
  numero: number;
  employe: Employe | null;
  typeOperation: string;
  occupe: boolean;
  estLeMien: boolean;
}

const SEUIL_RETRAIT_AGENCE = 4000;

@Component({
  selector: 'app-rendez-vous',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './rendez-vous.component.html',
  styleUrls: ['./rendez-vous.component.scss']
})
export class RendezVousComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

  private authService = inject(AuthService);
  private operationsService = inject(OperationsService);
  private operationsStore = inject(OperationsStoreService);
  private notificationService = inject(NotificationService);

  readonly SEUIL_RETRAIT = SEUIL_RETRAIT_AGENCE;

  // --- Etat opération ---
  montant = signal<number>(0);
  minMontant = signal<number>(0);
  typeOperation = signal<string>('DÉPÔT');
  numCompte = signal<string>('');

  // --- UI ---
  success = signal(false);
  agence = signal<string>('Bruxelles Centre — Grand Place');
  ticket = signal<string>('');
  currentTime = signal<string>(
    new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
  );

  // --- Guichets ---
  guichets = signal<Guichet[]>([]);
  loadingGuichets = signal<boolean>(false);
  errorGuichets = signal<string | null>(null);

  monGuichet = computed(() => this.guichets().find(g => g.estLeMien) ?? null);

  alerteRetraitGros = computed(() => {
    const m = this.rdvForm.get('montant')?.value ?? 0;
    return this.typeOperation() === 'RETRAIT' && (m ?? 0) >= SEUIL_RETRAIT_AGENCE;
  });

  // --- Form (simplifié : juste le montant) ---
  rdvForm = this.fb.group({
    montant: [0, [Validators.required, Validators.min(0.01)]]
  });

  ngOnInit() {
    const qp = this.route.snapshot.queryParams;
    if (qp['type']) this.typeOperation.set(qp['type']);
    if (qp['numCompte']) this.numCompte.set(qp['numCompte']);

    if (qp['montant']) {
      this.montant.set(Number(qp['montant']));
      this.rdvForm.patchValue({ montant: Number(qp['montant']) });
    }

    if (qp['minMontant']) {
      const min = Number(qp['minMontant']);
      this.minMontant.set(min);
      if (!qp['montant'] || Number(qp['montant']) < min) {
        this.rdvForm.patchValue({ montant: min });
      }
      this.rdvForm.get('montant')?.setValidators([Validators.required, Validators.min(min)]);
      this.rdvForm.get('montant')?.updateValueAndValidity();
    }

    this.ticket.set(this.genererTicket());
    this.loadGuichets();

    setInterval(() => {
      this.currentTime.set(
        new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
      );
    }, 30_000);
  }

  setTypeOperation(type: 'DÉPÔT' | 'RETRAIT') {
    if (this.typeOperation() === type) return;
    this.typeOperation.set(type);
    this.ticket.set(this.genererTicket());
    this.loadGuichets();
  }

  private normalizeTypeOperation(): string {
    return (this.typeOperation() || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }

  private genererTicket(): string {
    const lettre = this.normalizeTypeOperation().includes('retrait') ? 'R' : 'D';
    const num = Math.floor(Math.random() * 900) + 100;
    return `${lettre}${num}`;
  }

  loadGuichets() {
    const type = this.normalizeTypeOperation();
    let endpoint = '/api/employes/random';
    if (type.includes('depot')) endpoint = '/api/employes/random/depot';
    else if (type.includes('retrait')) endpoint = '/api/employes/random/retrait';

    this.loadingGuichets.set(true);
    this.errorGuichets.set(null);

    this.http.get<Employe>(endpoint).subscribe({
      next: (employe) => {
        const monNumero = Math.floor(Math.random() * 4) + 1;
        const guichets: Guichet[] = [1, 2, 3, 4].map(num => ({
          numero: num,
          employe: num === monNumero ? employe : null,
          typeOperation: num === monNumero ? this.typeOperation() : '—',
          occupe: num !== monNumero,
          estLeMien: num === monNumero
        }));
        console.log('[Guichet] Guichets:', guichets);
        this.guichets.set(guichets);
        this.loadingGuichets.set(false);
      },
      error: (err) => {
        console.error('[Guichet] Erreur:', err);
        this.errorGuichets.set('Aucun conseiller disponible pour le moment');
        this.guichets.set([]);
        this.loadingGuichets.set(false);
      }
    });
  }

  rafraichirGuichets() {
    this.ticket.set(this.genererTicket());
    this.loadGuichets();
  }

  getNomEmploye(g: Guichet): string {
    if (!g.employe) return 'Indisponible';
    return `${g.employe.prenom} ${g.employe.nom}`;
  }

  getInitiales(g: Guichet): string {
    if (!g.employe) return '?';
    return `${g.employe.prenom?.[0] ?? ''}${g.employe.nom?.[0] ?? ''}`.toUpperCase();
  }

  canSubmit(): boolean {
    return this.rdvForm.valid && this.monGuichet() !== null;
  }

  submitOperation() {
    if (!this.canSubmit()) {
      this.rdvForm.markAllAsTouched();
      return;
    }

    const guichet = this.monGuichet()!;
    const employe = guichet.employe;
    const montantOperation = this.rdvForm.value.montant!;
    const numCompte = this.numCompte();
    const typeOp = this.typeOperation();

    const conseillerText = employe
      ? `${employe.prenom} ${employe.nom} (${employe.matricule || 'N/A'})`
      : 'Non assigné';

    const description =
      `${typeOp} en agence — ${this.agence()} — Guichet ${guichet.numero} — ` +
      `Ticket ${this.ticket()} — Conseiller: ${conseillerText}`;

    let op;
    if (typeOp === 'DÉPÔT') {
      op = this.operationsService.deposit(numCompte, montantOperation, description);
    } else if (typeOp === 'RETRAIT') {
      op = this.operationsService.withdraw(numCompte, montantOperation, description);
    }
    if (!op) return;

    op.subscribe({
      next: () => {
        const now = new Date();
        if (typeOp === 'DÉPÔT') {
          this.operationsStore.addDepotWithDate(numCompte, montantOperation, description, now);
        } else if (typeOp === 'RETRAIT') {
          this.operationsStore.addRetraitWithDate(numCompte, montantOperation, description, now);
        }

        this.notificationService.addRDVNotification(
          now.toLocaleDateString('fr-FR'),
          now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }),
          typeOp,
          `${this.agence()} — Guichet ${guichet.numero}`,
          montantOperation
        );

        this.success.set(true);
        setTimeout(() => this.router.navigate(['/dashboard']), 3000);
      },
      error: (err) => {
        console.error('[Guichet] Erreur:', err);
        alert("Erreur lors de l'opération bancaire. Veuillez réessayer.");
      }
    });
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }

  formatMontant(m: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(m);
  }

  get montantErrorMessage(): string {
    const c = this.rdvForm.get('montant');
    if (c?.hasError('required')) return 'Le montant est requis';
    if (c?.hasError('min')) return `Minimum requis : ${this.formatMontant(this.minMontant())}`;
    return '';
  }
}