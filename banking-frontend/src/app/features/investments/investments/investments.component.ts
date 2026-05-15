import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import {
  Fonds,
  InvestmentService
} from '../investment.service';

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

  fonds = signal<Fonds[]>([]);
  selectedFonds = signal<Fonds | null>(null);

  compteId = signal<number | null>(null);
  numCompte = signal<string | null>(null);

  montant = signal<number | null>(null);

  loading = signal<boolean>(false);
  investing = signal<boolean>(false);
  error = signal<string>('');
  success = signal<string>('');

  /**
   * 🎯 AFFICHAGE DIRECT DE TOUS LES FONDS
   * On affiche TOUS les fonds reçus de l'API sans filtre restrictif.
   * Si l'API renvoie un fonds, c'est qu'il est disponible.
   * Filtre ultra-permissif : on cache UNIQUEMENT les fonds explicitement marqués inactifs.
   */
  fondsActifs = computed(() => {
    const liste = this.fonds();

    return liste.filter((f: any) => {
      // Si AUCUNE propriété "actif" n'est définie → on AFFICHE le fonds
      if (
        f.estActif === undefined &&
        f.actif === undefined &&
        f.disponible === undefined
      ) {
        return true;
      }

      // Si une propriété est explicitement à FALSE → on cache
      if (f.estActif === false || f.actif === false || f.disponible === false) {
        return false;
      }

      // Sinon (true, 1, null, autre) → on AFFICHE
      return true;
    });
  });

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const compteIdParam = params.get('compteId');
      const numCompteParam = params.get('numCompte');

      this.compteId.set(compteIdParam ? Number(compteIdParam) : null);
      this.numCompte.set(numCompteParam);
    });

    // ✅ Chargement IMMÉDIAT des fonds dès l'ouverture de la page
    this.loadFonds();
  }

  loadFonds(): void {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    this.investmentService.getFondsActifs().subscribe({
      next: (data: any) => {
        const rows = Array.isArray(data) ? data : data?.data ?? [];
        console.log('[FONDS] Reçus de l\'API:', rows);
        console.log('[FONDS] Nombre total:', rows.length);

        this.fonds.set(rows);
        this.loading.set(false);

        // Log de debug pour voir ce qui est filtré
        console.log('[FONDS] Affichés après filtre:', this.fondsActifs().length);
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
    this.montant.set(Number(fonds.montantMinimum || 0));
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

    if (montant < Number(fonds.montantMinimum || 0)) {
      this.error.set(`Le montant minimum pour ce fonds est ${fonds.montantMinimum} €.`);
      return;
    }

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
        this.success.set('Placement créé avec succès.');
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
    this.router.navigate(['/investissements/historique']);
  }

  retourDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  getCodeFonds(fonds: any): string {
    return fonds.codeFonds || fonds.codeIdentification || fonds.code_identification || 'N/A';
  }

  getRiskClass(risque: string | undefined): string {
    return (risque || '').toLowerCase();
  }
}