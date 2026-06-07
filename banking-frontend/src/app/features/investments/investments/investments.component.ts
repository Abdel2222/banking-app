import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Fonds, InvestmentService } from '../investment.service';
import { SelectedAccountService } from '../../../core/services/selected-account.service';

@Component({
  selector: 'app-investments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './investments.component.html',
  styleUrl: './investments.component.scss'
})
export class InvestmentsComponent implements OnInit {
  private readonly investmentService = inject(InvestmentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly selectedAccount = inject(SelectedAccountService);

  fonds = signal<Fonds[]>([]);
  selectedFonds = signal<Fonds | null>(null);

  compteId = signal<number | null>(null);
  numCompte = signal<string | null>(null);

  montant = signal<number | null>(null);

  loading = signal<boolean>(false);
  investing = signal<boolean>(false);
  error = signal<string>('');
  success = signal<string>('');

  fondsAffiches = computed(() => this.fonds());

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const compteIdParam = params.get('compteId');
      const numCompteParam = params.get('numCompte');

      if (compteIdParam) {
        const id = Number(compteIdParam);
        this.compteId.set(id);
        this.numCompte.set(numCompteParam);
        this.selectedAccount.set(id, numCompteParam);
      } else {
        // ✅ Fallback sur le service si pas de query params
        this.compteId.set(this.selectedAccount.compteId());
        this.numCompte.set(this.selectedAccount.numCompte());
      }
    });

    this.loadFonds();
  }

  loadFonds(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    this.investmentService.getTousLesFonds().subscribe({
      next: (data: any) => {
        const rows = Array.isArray(data) ? data : data?.data ?? [];
        this.fonds.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('[FONDS] Erreur chargement:', err);
        this.error.set('Impossible de charger les fonds de placement.');
        this.loading.set(false);
      }
    });
  }

  selectFonds(fonds: Fonds): void {
    this.selectedFonds.set(fonds);
    this.success.set('');
    this.error.set('');
    // ✅ Montant initialisé à 1 — plus de montant minimum
    this.montant.set(1);
  }

  updateMontant(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.montant.set(input.value ? Number(input.value) : null);
  }

  investir(): void {
    const compteId = this.compteId();
    const fonds = this.selectedFonds();
    const montant = this.montant();

    if (!compteId) {
      this.error.set('Aucun compte sélectionné. Retourne au dashboard et choisis un compte.');
      return;
    }
    if (!fonds) {
      this.error.set('Veuillez sélectionner un fonds.');
      return;
    }
    if (!montant || montant <= 0) {
      this.error.set('Veuillez saisir un montant valide.');
      return;
    }
    // ✅ Plus de validation montant minimum

    this.investing.set(true);
    this.error.set('');
    this.success.set('');

    this.investmentService.createPlacement({
      compteBancaireId: compteId,
      fondsId: fonds.id,
      montant: montant
    }).subscribe({
      next: () => {
        this.investing.set(false);
        this.success.set('✅ Placement créé avec succès !');
        this.montant.set(null);
        this.selectedFonds.set(null);
      },
      error: (err) => {
        this.investing.set(false);
        this.error.set(err?.error?.message || 'Impossible de créer le placement.');
      }
    });
  }

  goToHistory(): void {
    const id = this.compteId();
    const num = this.numCompte();
    this.router.navigate(['/investissements/historique'], {
      queryParams: id ? { compteId: id, numCompte: num } : {}
    });
  }

  retourDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  getCodeFonds(fonds: any): string {
    return fonds?.codeIdentification || fonds?.codeFonds || 'N/A';
  }

  getStartingHint(fonds: any): string {
    const code = (fonds?.codeIdentification || '').toUpperCase();
    if (code.startsWith('FDS-SEC')) return '🛡️ Idéal pour débuter en toute sérénité';
    if (code.startsWith('FDS-EQ'))  return '⚖️ Le bon équilibre entre prudence et rendement';
    if (code.startsWith('FDS-CRY')) return '🚀 Plongez dans l\'univers crypto avec ce ticket d\'entrée';
    if (code.startsWith('FDS-TEC')) return '💡 Investissez dans l\'innovation et la tech de demain';
    if (code.startsWith('FDS-IMM')) return '🏠 Démarrez votre patrimoine immobilier';
    if (code.startsWith('FDS-TER')) return '🌾 Placement long terme avec belle plus-value à la clé';
    if (code.startsWith('FDS-ENE')) return '🌱 Soutenez la transition écologique';
    return '✨ Un montant de départ accessible pour démarrer';
  }
}